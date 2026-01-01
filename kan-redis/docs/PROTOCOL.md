# Protocol Specification

## Overview
Kan-Redis uses a custom **Binary TLV (Type-Length-Value)** protocol. This minimizes CPU overhead by avoiding String parsing/regex (unlike text protocols like HTTP/JSON).

## Data Types
* **Byte:** 1 byte (Command Codes, Status)
* **Int:** 4 bytes (Lengths - Big Endian)
* **Raw Bytes:** Variable length (Key/Value payloads)

## Command Frames

### 1. SET
Stores a value.
`[CMD=0x02]` `[KeyLen]` `[Key]` `[ValLen]` `[Value]`
* **Response:** `[Len=1]` `[Status=1 (OK)]`

### 2. GET
Retrieves a value.
`[CMD=0x01]` `[KeyLen]` `[Key]`
* **Response (Found):** `[Len=N]` `[Value]`
* **Response (Null):** `[Len=0]`

### 3. CAS (Compare-And-Swap)
Atomic update.
`[CMD=0x03]` `[KeyLen]` `[Key]` `[ExpLen]` `[ExpectedVal]` `[NewLen]` `[NewVal]`
* **Response:**
    * `[Len=1]` `[Status=1 (Success)]`
    * `[Len=1]` `[Status=0 (Collision/Fail)]`

## Error Handling
If the server receives a malformed packet or unknown command:
* **Response:** `[Len=-1] (Error Flag)`