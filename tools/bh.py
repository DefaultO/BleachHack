#!/usr/bin/env python
"""Talk to BleachHack's DevBridge. Usage: python bh.py <command...>"""
import socket
import sys

PORT = 26501


def send(line, timeout=20.0):
    with socket.create_connection(("127.0.0.1", PORT), timeout=timeout) as s:
        s.sendall((line + "\n").encode("utf-8"))
        s.shutdown(socket.SHUT_WR)
        chunks = []
        while True:
            data = s.recv(65536)
            if not data:
                break
            chunks.append(data)
    return b"".join(chunks).decode("utf-8", "replace")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: bh.py <command...>")
        sys.exit(2)
    try:
        print(send(" ".join(sys.argv[1:])).rstrip())
    except Exception as e:
        print("BRIDGE-ERR", e)
        sys.exit(1)
