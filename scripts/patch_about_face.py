#!/usr/bin/env python3
"""tool.face 扩容手术：把「关于」页 Lua（含头像 blob）写入手环端 Lua 区。

背景（详见 skill 01-face-format.md）：
  - 头部条目表 @280/@296/@312 = {记录起始 u32, 记录长度 u32, 类型 u32=0x05000002, 0}
    三条资源：lua/all.fpk(336) / lua/all_restore.fpk(1668540) / lua/main.lua(3336752)
  - main.lua 记录 = [3336752, 3353794)，其中数据区 [3336784, 3353794) = 17010B
  - 尾表起始于 3353796（头部 u32@32 与 u32@172 都指向它）

手术内容（Δ = 5120）：
  1. main.lua 数据区扩到 17010+Δ，内容 = Lua源码 + 空格填充 + "--[[\\n" + 头像blob×2 + "]]"
     （blob 在 Lua 长注释里，编译合法；运行时读自身文件末尾取出）
  2. 条目@312 的长度 += Δ；头部 u32@32 / u32@172（尾表指针）+= Δ；尾表整体后移 Δ。
  3. all.fpk / all_restore.fpk 记录与数据不动。
"""
import struct
import subprocess
import sys

FACE = "/workspace/band10-icon-tool/app/src/main/assets/tool.face"
LUA = "/workspace/band10-icon-tool/scripts/watch_ui/tool_main.lua"
B1F = "/workspace/band10-icon-tool/scripts/about_assets/av_dienuo.i4"
B2F = "/workspace/band10-icon-tool/scripts/about_assets/av_qq.i4"

LUA_OFF = 3336784          # main.lua 数据区起点
REC_START = 3336752        # main.lua 记录起点
OLD_TRAILER = 3353796      # 尾表原起点（u32@32 / u32@172）
OLD_REGION = OLD_TRAILER - 2 - LUA_OFF   # 17010
DELTA = 5120
REGION = OLD_REGION + DELTA              # 22130
BLOB = 1228
MARK = b"--[[\n"            # 6B
CLOSE = b"]]"               # 2B

def main():
    d = open(FACE, "rb").read()
    src = open(LUA, "rb").read()
    b1 = open(B1F, "rb").read()
    b2 = open(B2F, "rb").read()
    assert len(b1) == BLOB and len(b2) == BLOB
    assert src.find(b"return ui") > 0

    # 结构自检（模板必须仍是 v2.2.3 布局）
    assert len(d) == 3361548, len(d)
    assert struct.unpack("<I", d[32:36])[0] == OLD_TRAILER
    assert struct.unpack("<I", d[172:176])[0] == OLD_TRAILER
    st, ex, ty, pd = struct.unpack("<IIII", d[312:328])
    assert (st, ty, pd) == (REC_START, 0x05000002, 0) and ex == OLD_REGION + 32, (st, ex, ty)
    assert d[LUA_OFF:LUA_OFF + 4] == b"--[["  # 旧 Lua 以注释开头

    # 组装新 Lua 区：源码 + 空格 + 长注释(blob) ，总长恰好 REGION
    assert b"]]" not in b1 and b"]]" not in b2, "blob 含 ]] 会提前闭合长注释"
    fixed = len(src) + len(MARK) + BLOB * 2 + len(CLOSE)
    pad = REGION - fixed
    assert pad >= 0, f"超出容量：需要 {fixed} > {REGION}（Δ={DELTA} 不够）"
    chunk = src + b" " * pad + MARK + b1 + b2 + CLOSE
    assert len(chunk) == REGION

    # 重新拼文件
    out = bytearray(d)
    # 1) 头部指针与条目长度
    struct.pack_into("<I", out, 32, OLD_TRAILER + DELTA)
    struct.pack_into("<I", out, 172, OLD_TRAILER + DELTA)
    struct.pack_into("<I", out, 316, ex + DELTA)
    # 2) main.lua 数据区整体替换
    out[LUA_OFF:OLD_TRAILER - 2] = chunk
    # 3) 尾表随记录区扩展自然后移（chunk 已比原区域长 DELTA），无需再插入
    out = bytes(out)

    assert len(out) == 3361548 + DELTA, len(out)

    # 校验
    assert out[367:1668579] == d[367:1668579], "fpk1 被改动"
    assert out[1668579:3336752] == d[1668579:3336752], "fpk2/记录头被改动"
    nc = bytes(out)
    assert struct.unpack("<I", nc[312 + 4:312 + 8])[0] == ex + DELTA
    assert struct.unpack("<I", nc[32:36])[0] == OLD_TRAILER + DELTA
    got = nc[LUA_OFF:LUA_OFF + REGION]
    assert got == chunk
    assert got[-2:] == CLOSE
    assert got[-2 - BLOB:-2] == b2 and got[-2 - 2 * BLOB:-2 - BLOB] == b1
    # 尾表平移后内容一致
    assert nc[OLD_TRAILER + DELTA:] == d[OLD_TRAILER:]

    open(FACE, "wb").write(nc)
    # luac 语法校验整个 chunk（源码+注释+padding 必须是合法 Lua）
    open("/tmp/chunk_check.lua", "wb").write(chunk)
    subprocess.run(["luac", "-p", "/tmp/chunk_check.lua"], check=True)
    print(f"OK: face {len(d)} -> {len(nc)} (+{DELTA})，Lua 区 {REGION}B，"
          f"源码 {len(src)}B + blob {BLOB*2}B + pad {pad}B")

if __name__ == "__main__":
    main()
