#!/usr/bin/env python
"""Source RCON client for the dev server. Usage: python rcon.py "<command>" """
import socket
import struct
import sys

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "bleachdev"

SERVERDATA_AUTH = 3
SERVERDATA_EXECCOMMAND = 2


def _pack(req_id, req_type, body):
    payload = struct.pack("<ii", req_id, req_type) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def _read(sock):
    raw_len = sock.recv(4)
    if len(raw_len) < 4:
        return None, None, ""
    (length,) = struct.unpack("<i", raw_len)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            break
        data += chunk
    req_id, req_type = struct.unpack("<ii", data[:8])
    return req_id, req_type, data[8:-2].decode("utf-8", "replace")


def run(command, timeout=10.0):
    with socket.create_connection((HOST, PORT), timeout=timeout) as s:
        s.sendall(_pack(1, SERVERDATA_AUTH, PASSWORD))
        req_id, _, _ = _read(s)
        if req_id == -1:
            raise RuntimeError("RCON auth failed")
        s.sendall(_pack(2, SERVERDATA_EXECCOMMAND, command))
        _, _, body = _read(s)
        return body


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: rcon.py \"<command>\"")
        sys.exit(2)
    try:
        out = run(" ".join(sys.argv[1:]))
        print(out.strip() if out.strip() else "(no output)")
    except Exception as e:
        print("RCON-ERR", e)
        sys.exit(1)
