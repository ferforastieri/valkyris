package store

import (
	"context"
	"database/sql"
	"testing"

	_ "modernc.org/sqlite"
)

func TestOpenMigratesONVIFServiceAddresses(t *testing.T) {
	path := t.TempDir() + "/legacy.db"
	legacy, err := sql.Open("sqlite", path)
	if err != nil {
		t.Fatal(err)
	}
	_, err = legacy.Exec(`CREATE TABLE cameras (
      id TEXT PRIMARY KEY, name TEXT NOT NULL, host TEXT NOT NULL, port INTEGER NOT NULL DEFAULT 2020,
      username_enc BLOB NOT NULL, password_enc BLOB NOT NULL, rtsp_uri_enc BLOB NOT NULL,
      profile_token TEXT NOT NULL DEFAULT '', capabilities_json TEXT NOT NULL DEFAULT '{}',
      enabled INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL, updated_at TEXT NOT NULL
    )`)
	legacy.Close()
	if err != nil {
		t.Fatal(err)
	}
	db, err := Open(path)
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	for _, column := range []string{"media_xaddr", "events_xaddr", "ptz_xaddr"} {
		exists, checkErr := columnExists(context.Background(), db.DB, "cameras", column)
		if checkErr != nil || !exists {
			t.Fatalf("column %s was not migrated: %v", column, checkErr)
		}
	}
}
