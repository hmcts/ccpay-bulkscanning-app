#!/usr/bin/env python3

import sys, json, base64, hmac, hashlib, time

def generate_totp(client_secret):
    key = base64.b32decode(client_secret.upper())
    timestamp = int(time.time() // 30)
    msg = timestamp.to_bytes(8, 'big')
    hmac_hash = hmac.new(key, msg, hashlib.sha1).digest()
    offset = hmac_hash[-1] & 0xf
    code = (int.from_bytes(hmac_hash[offset:offset+4], 'big') & 0x7fffffff) % 1000000
    return f"{code:06}"

if __name__ == "__main__":
    input_data = json.load(sys.stdin)
    client_secret = input_data.get("client_secret")
    if not client_secret:
        raise ValueError("client_secret is required")

    result = {"one_time_password": generate_totp(client_secret)}
    print(json.dumps(result))
