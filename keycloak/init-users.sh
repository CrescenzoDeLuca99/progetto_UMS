#!/bin/bash
set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8180}"
REALM="user-management"

echo ">>> Configurazione credenziali admin..."
/opt/keycloak/bin/kcadm.sh config credentials \
  --server "$KEYCLOAK_URL" \
  --realm master \
  --user admin \
  --password admin \
  --client admin-cli

create_user() {
  local username=$1
  local password=$2
  local role=$3

  echo ">>> Creazione utente: $username"
  /opt/keycloak/bin/kcadm.sh create users -r "$REALM" \
    -s username="$username" \
    -s firstName="$username" \
    -s lastName="test" \
    -s email="$username@test.local" \
    -s emailVerified=true \
    -s enabled=true \
    -s 'requiredActions=[]' 2>/dev/null || echo "    Utente $username già esistente, skip."

  echo ">>> Impostazione password per: $username"
  /opt/keycloak/bin/kcadm.sh set-password -r "$REALM" \
    --username "$username" \
    --new-password "$password"

  echo ">>> Assegnazione ruolo $role a: $username"
  /opt/keycloak/bin/kcadm.sh add-roles -r "$REALM" \
    --uusername "$username" \
    --rolename "$role"

  echo ">>> Pulizia required actions per: $username"
  USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r "$REALM" -q username="$username" --fields id --format csv --noquotes)
  /opt/keycloak/bin/kcadm.sh update users/"$USER_ID" -r "$REALM" -s 'requiredActions=[]'
}

create_user "admin-user"    "admin"     "OWNER"
create_user "operator-user" "operator"  "OPERATOR"
create_user "dev-user"      "developer" "DEVELOPER"

echo ">>> Utenti Keycloak inizializzati con successo."
