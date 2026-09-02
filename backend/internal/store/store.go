package store

import (
	"context"
	"database/sql"
	_ "embed"
	"fmt"
	"time"

	_ "modernc.org/sqlite"
)

//go:embed schema.sql
var schema string

type Store struct{ DB *sql.DB }

func Open(path string) (*Store, error) {
	db, err := sql.Open("sqlite", path+"?_pragma=busy_timeout(5000)&_pragma=foreign_keys(1)")
	if err != nil {
		return nil, fmt.Errorf("open database: %w", err)
	}
	db.SetMaxOpenConns(1)
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if _, err = db.ExecContext(ctx, schema); err != nil {
		db.Close()
		return nil, fmt.Errorf("apply schema: %w", err)
	}
	for _, migration := range []struct {
		column string
		query  string
	}{
		{"media_xaddr", `ALTER TABLE cameras ADD COLUMN media_xaddr TEXT NOT NULL DEFAULT ''`},
		{"events_xaddr", `ALTER TABLE cameras ADD COLUMN events_xaddr TEXT NOT NULL DEFAULT ''`},
		{"ptz_xaddr", `ALTER TABLE cameras ADD COLUMN ptz_xaddr TEXT NOT NULL DEFAULT ''`},
	} {
		exists, migrationErr := columnExists(ctx, db, "cameras", migration.column)
		if migrationErr != nil {
			db.Close()
			return nil, migrationErr
		}
		if !exists {
			if _, migrationErr = db.ExecContext(ctx, migration.query); migrationErr != nil {
				db.Close()
				return nil, fmt.Errorf("migrate cameras.%s: %w", migration.column, migrationErr)
			}
		}
	}
	return &Store{DB: db}, nil
}

func columnExists(ctx context.Context, db *sql.DB, table, column string) (bool, error) {
	rows, err := db.QueryContext(ctx, "PRAGMA table_info("+table+")")
	if err != nil {
		return false, err
	}
	defer rows.Close()
	for rows.Next() {
		var cid, notNull, primaryKey int
		var name, kind string
		var defaultValue any
		if err = rows.Scan(&cid, &name, &kind, &notNull, &defaultValue, &primaryKey); err != nil {
			return false, err
		}
		if name == column {
			return true, nil
		}
	}
	return false, rows.Err()
}

func (s *Store) Close() error { return s.DB.Close() }

func NullTime(value sql.NullString) *time.Time {
	if !value.Valid {
		return nil
	}
	t, err := time.Parse(time.RFC3339Nano, value.String)
	if err != nil {
		return nil
	}
	return &t
}
