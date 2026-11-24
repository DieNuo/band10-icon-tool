#!/usr/bin/env python3
"""压缩 tool_main.lua 里的 ENT 数据表以腾出 Lua 区空间：
  - off 十进制 -> 36 进制
  - len -> 唯一值索引
  - 路径去 '/launcher.bin' 后缀（解析时补回）
  - 第二颜色默认 00000000（省略）
  - 解析代码同步改写
省约 1300B，解析出的 ENT 与原版逐字段一致。
"""
import re
from pathlib import Path

P = Path(__file__).resolve().parent / "watch_ui/tool_main.lua"
B36 = "0123456789abcdefghijklmnopqrstuvwxyz"

def to_b36(n):
    n = int(n); s = ""
    while n:
        n, r = divmod(n, 36); s = B36[r] + s
    return s or "0"

s = P.read_text(encoding="utf-8")
m = re.search(r'local src = "(.*?)"\n', s, re.S)
data = m.group(1)
lines = data.split('\\n')
lens = sorted(set(l.split('|')[2] for l in lines), key=lambda x: int(x))
print("len 唯一值:", lens)

new_lines = []
for l in lines:
    p, off, ln, smp = l.split('|')
    p = p.replace('/launcher.bin', '')  # 解析时补回
    a, b, c = smp.split(',')
    sfx = '' if c == '00000000' else ',' + c
    new_lines.append(f"{p}|{to_b36(off)}|{lens.index(ln)}|{a},{b}{sfx}")

new_data = '\\n'.join(new_lines)
print(f"ENT 数据: {len(data.encode())}B -> {len(new_data.encode())}B (省 {len(data.encode())-len(new_data.encode())}B)")

# 新解析代码
new_parse = '''local LV={@LENS@}
  local function b36(t) local n=0 for i=1,#t do local d=t:byte(i); n=n*36+(d<58 and d-48 or d-87) end return n end
  for line in src:gmatch("[^\\n]+") do
    local path,off,li,smp = line:match("([^|]+)|([^|]+)|([^|]+)|(.+)")
    if not path:find("%.") then path=path.."/launcher.bin" end
    local a,b,c = smp:match("([^,]+),([^,]+),?(.*)")
    if c=="" then c="00000000" end
    ENT[#ENT+1] = { path, b36(off), LV[tonumber(li)+1], { {tonumber(a), b, c} } }
  end'''.replace("@LENS@", ','.join(lens))

old_parse = '''for line in src:gmatch("[^\\n]+") do
    local path, off, ln, smp = line:match("([^|]+)|([^|]+)|([^|]+)|(.+)")
    local s = {}
    for a, b, c in smp:gmatch("([^,;]+),([^,;]+),([^,;]+)") do
      s[#s + 1] = { tonumber(a), b, c }
    end
    ENT[#ENT + 1] = { path, tonumber(off), tonumber(ln), s }
  end'''

assert old_parse in s, "旧解析代码未找到"
s = s[:m.start(1)] + new_data + s[m.end(1):]
s = s.replace(old_parse, new_parse)
P.write_text(s, encoding="utf-8")
print("写入完成, 新文件大小:", len(s.encode()), "B")
