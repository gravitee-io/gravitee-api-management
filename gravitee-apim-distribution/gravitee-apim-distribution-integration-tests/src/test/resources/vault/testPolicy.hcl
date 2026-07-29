path "secret/*" {
  capabilities = [
    "read",
    "list"]
}

path "kv-v1/*" {
  capabilities = [
    "read",
    "list"]
}

path "auth/token/lookup-self" {
  capabilities = [
    "read"]
}
