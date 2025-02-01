# Auth Server API Documentation
This documentation provides docs for every authorization server endpoint.
Internally by the controller and every official droplet, auth tokens are used for authentication. No GRPC request
with invalid auth token is possible.

# Scoping
Scoping is the OAuth way to deal with permissions. Scopes are represented in a string, seperated by a whitespace.
In our case, they can also be wildcarded (`*`).

**These scopes:**
  - `test.*`
  - `other.test`

**will be passed as:** `test.* other.test` on auth server rest endpoints.

The scope `*` will grant every permission.

# Getting scopes in a GRPC context
You can get the scopes that are provided to the current context (but ony if this context uses the v3 controllers `AuthSecretInterceptor`) like this:

```kt
val scopes = MetadataKeys.SCOPES.get() // List<String>
```

# Restricted endpoints
For some endpoints, there are listed authorization scope requirements.
To access these endpoints, you must pass a token in the Authorization header that meets these requirements.

### Header example
```json
 {
  "Authorization": "Bearer <token>" 
 }
```

You can gain access to an auth token by using the master token, retrieving a token through user login (`/login`)
and by creating a custom client with the `client_credentials` method and calling the (`/token`) endpoint to log in with this client.

# API Documentation for Authorization Endpoints

The `AuthorizationHandler` provides various endpoints for client registration, authorization, token requests, revocation, and introspection. Below is a detailed description of each endpoint.

---

## Register Client

### Endpoint
`POST /oauth/register_client`

### Description
Registers a new OAuth client, associating it with a unique client secret, grant types, and scope.

### Request Parameters
- **`master_token`** (String, required): The master token for authentication (in the `.secrets/auth.secret` file).
- **`client_id`** (String, required): The unique identifier for the client.
- **`redirect_uri`** (String, required): The URI to redirect to after authorization.
- **`grant_types`** (String, required): The grant types supported by the client.
- **`scope`** (String, optional): The scopes the client has access to.
- **`client_secret`** (String, optional): The client secret. If not provided, a random one will be generated.

### Authorization Scope Required
- None

### Responses
- **200 OK**: The client was successfully registered.  
  ```json
  {
    "client_id": "client_id",
    "client_secret": "client_secret"
  }
  ```
- **400 Bad Request**: Missing required parameters such as `client_id` or `grant_types`.
- **403 Forbidden**: Invalid master token.

---

## Authorize Request

### Endpoint
`POST /oauth/authorize`

### Description
Handles the authorization request for an OAuth client with PKCE (Proof Key for Code Exchange) support.

### Request Parameters
- **`client_id`** (String, required): The unique identifier for the client.
- **`redirect_uri`** (String, required): The URI to redirect to after authorization.
- **`code_challenge_method`** (String, required): The challenge method. Must be `S256`.
- **`code_challenge`** (String, required): The PKCE code challenge.
- **`scope`** (String, required): The requested scope.

### Authorization Scope Required
- None

### Responses
- **200 OK**: Authorization successful.  
  ```json
  {
    "redirectUri": "<redirect_uri>?code=<authorization_code>"
  }
  ```
- **400 Bad Request**: Missing required parameters such as `client_id`, `redirect_uri`, `scope`, or `code_challenge`.
- **404 Not Found**: Client not found.
- **400 Bad Request**: Invalid challenge or unsupported grant types.

---

## Token Request

### Endpoint
`POST /oauth/token`

### Description
Handles the token request to exchange the authorization code for an access token or to generate a client credentials token.

### Request Parameters
- **`client_id`** (String, required): The unique identifier for the client.
- **`client_secret`** (String, required): The client secret.
- **`code`** (String, required if using authorization code flow): The authorization code.
- **`code_verifier`** (String, required if using PKCE): The PKCE code verifier.

### Authorization Scope Required
- None

### Responses
- **200 OK**: Token successfully issued.
  ```json
  {
    "access_token": "access_token",
    "scope": "scope",
    "exp": "expiration_time",
    "user_id": "user_id",
    "client_id": "client_id"
  }
  ```
- **400 Bad Request**: Missing required parameters such as `client_id` or `client_secret`.
- **404 Not Found**: Client not found.
- **400 Bad Request**: Invalid client secret or unsupported grant type.

---

## Revoke Token

### Endpoint
`POST /oauth/revoke`

### Description
Revokes an OAuth token, rendering it inactive.

### Request Parameters
- **`access_token`** (String, required): The access token to revoke.

### Authorization Scope Required
- None

### Responses
- **200 OK**: The token was successfully revoked.
- **400 Bad Request**: Invalid access token.
- **500 Internal Server Error**: Could not delete token.

---

## Introspect Token

### Endpoint
`POST /oauth/introspect`

### Description
Introspects a token to verify its validity and return token details.

### Request Parameters
- **`token`** (String, required): The token to introspect.

### Authorization Scope Required
- None

### Responses
- **200 OK**: Token is valid and active.  
  ```json
  {
    "active": true,
    "token_id": "token_id",
    "client_id": "client_id",
    "scope": "scope",
    "exp": "expiration_time"
  }
  ```
- **200 OK**: Token is invalid or expired.  
  ```json
  {
    "active": false
  }
  ```
- **400 Bad Request**: Token is missing.

# Authentication Endpoints

The `AuthenticationHandler` provides various endpoints to manage OAuth groups, users, and tokens. Below is a detailed
description of each endpoint.

---

## Save Group

### Endpoint

`PUT /group`

### Description

Creates or updates an OAuth group with the specified scopes.

### Request Parameters

- **`group_name`** (String, required): Name of the group.
- **`scopes`** (String, optional): Space-separated list of scopes for the group.

### Authorization Scope Required

- `simplecloud.auth.group.save.<group_name>`

### Responses

- **200 OK**: Success message.
- **400 Bad Request**: You must specify a group name.
- **401 Unauthorized**: Unauthorized.

---

## Get Group

### Endpoint

`GET /group`

### Description

Retrieves details of a specific OAuth group.

### Request Parameters

- **`group_name`** (String, required): Name of the group to retrieve.

### Authorization Scope Required

- `simplecloud.auth.group.get.<group_name>`

### Responses

- **200 OK**: Group details.
  ```json
  {
    "group_name": "example_group",
    "scope": "read write"
  }
  ```
- **400 Bad Request**: You must specify a group name.
- **404 Not Found**: Group not found.
- **401 Unauthorized**: Unauthorized.

---

## Get All Groups

### Endpoint

`GET /groups`

### Description

Fetches a list of all OAuth groups.

### Authorization Scope Required

- `simplecloud.auth.group.get.*`

### Responses

- **200 OK**: List of all groups.
  ```json
  [
    {
      "group_name": "group1",
      "scope": "read write"
    },
    {
      "group_name": "group2",
      "scope": "read"
    }
  ]
  ```
- **401 Unauthorized**: Unauthorized.

---

## Delete Group

### Endpoint

`DELETE /group`

### Description

Deletes a specific OAuth group.

### Request Parameters

- **`group_name`** (String, required): Name of the group to delete.

### Authorization Scope Required

- `simplecloud.auth.group.delete.<group_name>`

### Responses

- **200 OK**: Success message.
- **400 Bad Request**: You must specify a group name.
- **404 Not Found**: Group not found.
- **401 Unauthorized**: Unauthorized.

---

## Save User

### Endpoint

`PUT /user`

### Description

Creates or updates a user with the specified groups and scopes.

### Request Parameters

- **`username`** (String, required): The username.
- **`password`** (String, required): The password.
- **`groups`** (String, optional): Space-separated list of groups the user belongs to.
- **`scope`** (String, optional): Space-separated list of scopes for the user.

### Authorization Scope Required

- `simplecloud.auth.user.save`

### Responses

- **200 OK**: Success message.
- **400 Bad Request**: You must specify a username or password.
- **401 Unauthorized**: Unauthorized.

---

## Get User

### Endpoint

`GET /user`

### Description

Fetches details of a specific user.

### Request Parameters

- **`username`** (String, required): Name of the user to retrieve.

### Authorization Scope Required

- `simplecloud.auth.user.get.<username>`

### Responses

- **200 OK**: User details.
  ```json
    {
      "user_id": "1234",
      "username": "example_user",
      "scope": "read write",
      "groups": "group1 group2"
    }
  ```
- **400 Bad Request**: You must specify a username.
- **404 Not Found**: User not found.
- **401 Unauthorized**: Unauthorized.

---

## Get All Users

### Endpoint

`GET /users`

### Description

Fetches a list of all users.

### Authorization Scope Required

- `simplecloud.auth.user.get.*`

### Responses

- **200 OK**: List of all users.
  ```json
  [
    {
      "user_id": "1234",
      "username": "user1",
      "scope": "read write",
      "groups": "group1 group2"
    },
    {
      "user_id": "5678",
      "username": "user2",
      "scope": "read",
      "groups": "group3"
    }
  ]
  ```
- **401 Unauthorized**: Unauthorized.

---

## Delete User

### Endpoint

`DELETE /user`

### Description

Deletes a specific user.

### Request Parameters

- **`user_id`** (String, required): The user ID to delete.

### Authorization Scope Required

- `simplecloud.auth.user.delete`

### Responses

- **200 OK**: Success message.
- **400 Bad Request**: You must specify a user ID.
- **404 Not Found**: User not found.
- **401 Unauthorized**: Unauthorized.

---

## Login

### Endpoint

`POST /login`

### Description

Authenticates a user and returns an access token if the username and password are valid.

### Request Parameters

- **`username`** (String, required): The username.
- **`password`** (String, required): The password.

### Responses

- **200 OK**: Access token and user details.
  ```json
  {
    "access_token": "jwt_token",
    "scope": "read write",
    "exp": 3600,
    "user_id": "1234",
    "client_id": "abcd"
  }
  ```
- **400 Bad Request**: You must specify a username and password.
- **401 Unauthorized**: Invalid username or password.
