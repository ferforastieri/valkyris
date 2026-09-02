package crypto

import (
	"bytes"
	"testing"
)

func TestVaultRoundTrip(t *testing.T) {
	key := bytes.Repeat([]byte{7}, 32)
	path := t.TempDir() + "/key"
	if err := osWriteFile(path, key); err != nil {
		t.Fatal(err)
	}
	v, err := LoadOrCreate(path)
	if err != nil {
		t.Fatal(err)
	}
	sealed, err := v.EncryptString("camera-secret")
	if err != nil {
		t.Fatal(err)
	}
	if bytes.Contains(sealed, []byte("camera-secret")) {
		t.Fatal("plaintext leaked")
	}
	plain, err := v.DecryptString(sealed)
	if err != nil || plain != "camera-secret" {
		t.Fatalf("round trip: %q %v", plain, err)
	}
}
