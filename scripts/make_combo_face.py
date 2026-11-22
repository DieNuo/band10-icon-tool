#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 tool.face 生成「副本工具」combo.face：建副本 + 删副本 二合一。

  - 建副本：/system/app.bin → /data/fpatch/app.bin，逐块复制+校验
  - 删副本：二次确认后删除副本，释放 42MB
  - 检查状态：显示源/副本大小与就绪状态

与 init.face / del.face 完全相同的等长替换技术：
  - Lua 文本区 [3336784, 3336784+16758) 等长替换，尾部 284B 原样保留
  - 头部应用名 "换装工具"(12B) → "副本工具"(12B)，定长字段内替换
"""
import sys

SRC_FACE = "app/src/main/assets/tool.face"
OUT_FACE = "/workspace/环10副本工具_建删二合一.face"

TOTAL = 42667008
LUA_TEXT_LEN = 16758

NEW_LUA = r'''--[[
 FPtool · 副本工具（建删二合一）
     换装表盘应用封面之前，需要手环上有一份 /system/app.bin 的完整副本
     （/data/fpatch/app.bin，42667008 字节，约42MB）。没有它会提示「缺少副本」。
     本表盘两件事都能做：
       建副本 —— 第一次使用/删过之后，建出这份副本
       删副本 —— 不再玩换装、想腾空间时删掉它（二次确认防误触）
]]
local lvgl = require("lvgl")

local DIR   = "/data/fpatch"
local SRC   = "/system/app.bin"
local DST   = DIR .. "/app.bin"
local TOTAL = 42667008
local W, H  = 212, 520

local function sh(cmd)
  local a, b, c = os.execute(cmd)
  if type(a) == "number" then return a end
  if a == true then return 0 end
  if type(b) == "number" then return b end
  return -1
end
local function size_of(p)
  local f = io.open(p, "rb")
  if not f then return -1 end
  local n = f:seek("end") or -1
  f:close()
  return n
end

-- ---------------------------------------------------------------- UI
local root, label, plate, concl, F_BODY, F_HEAD, F_BIG, F_TITLE
pcall(function()
  F_BODY = lvgl.Font("MiSans-Regular", 9)
  F_HEAD = lvgl.Font("MiSans-Regular", 14)
  F_BIG  = lvgl.Font("MiSans-Regular", 13)
  F_TITLE = lvgl.Font("MiSans-Regular", 18)
end)
pcall(function()
  root = lvgl.Object(nil, { x = 0, y = 0, w = W, h = H,
    bg_color = "#0E1116", bg_opa = lvgl.OPA(100),
    border_width = 0, radius = 0, pad_all = 0 })
  root:clear_flag(lvgl.FLAG.SCROLLABLE)
end)

local buf = {}
local function say(s)
  buf[#buf + 1] = tostring(s)
  while #buf > 3 do table.remove(buf, 1) end
  if label then pcall(function() label:set { text = table.concat(buf, "\n") } end) end
end
local function reset()
  buf = {}
  if label then pcall(function() label:set { text = "" } end) end
end
local function verdict(t, c)
  if concl then pcall(function() concl:set { text = t, text_color = c } end) end
  if plate then
    local bg = 0x161b22
    if c == "#63D9A0" then bg = 0x0d3a2a
    elseif c == "#ff6b6b" then bg = 0x3a1414
    elseif c == "#f0c060" then bg = 0x3a2f14 end
    pcall(function() plate:set { bg_color = bg } end)
  end
end
local function mkbtn(y, h, text, bg, fs, cb)
  local ok, obj = pcall(function()
    return lvgl.Object(root, { x = 10, y = y, w = 192, h = h,
      bg_color = bg, bg_opa = lvgl.OPA(100),
      border_width = 1, border_color = "#2C3A48", radius = 9, pad_all = 0 })
  end)
  if ok and obj then
    pcall(function() obj:clear_flag(lvgl.FLAG.SCROLLABLE) end)
    pcall(function() obj:add_flag(lvgl.FLAG.CLICKABLE) end)
    pcall(function() obj:add_flag(lvgl.FLAG.EVENT_BUBBLE) end)
    pcall(function()
      local lb = lvgl.Label(obj, { x = 0, y = 0, w = 192, h = h, text = text,
        text_font = fs or F_BODY, text_color = "#FFFFFF", text_align = 2 })
      lb:clear_flag(lvgl.FLAG.CLICKABLE)
      lb:add_flag(lvgl.FLAG.EVENT_BUBBLE)
    end)
    pcall(function() obj:onevent(lvgl.EVENT.CLICKED, function() pcall(cb) end) end)
  end
  return obj
end

-- ---------------------------------------------------------------- 复制
local function copyBin()
  local fin = io.open(SRC, "rb")
  if not fin then return -1, "打不开 /system/app.bin" end
  local fout = io.open(DST, "wb")
  if not fout then fin:close() return -2, "副本写不了，存储可能不足" end
  local total = 0
  local ok, err = pcall(function()
    while true do
      local blk = fin:read(65536)
      if not blk or #blk == 0 then break end
      fout:write(blk)
      total = total + #blk
    end
  end)
  fin:close()
  fout:close()
  if not ok then return total, "复制中断：" .. tostring(err or "空间不足") end
  return total, nil
end

-- ---------------------------------------------------------------- 删除
local armed = false
local function doDelete()
  reset()
  local s = size_of(DST)
  if s < 0 then
    verdict("本来就没有副本", "#63D9A0")
    say("空间已经是干净的")
    armed = false
    return
  end
  say(string.format("正在删除 %d 字节…", s))
  pcall(function() os.remove(DST) end)
  sh("sync")
  if size_of(DST) < 0 then
    verdict("已删除", "#63D9A0")
    say("释放了约 42MB 空间")
    say("换装前请先建副本")
  else
    verdict("删除失败", "#ff6b6b")
    say("副本仍在，可能被占用")
    say("重启手环后再试")
  end
  armed = false
end

-- ---------------------------------------------------------------- 界面
if root then
  pcall(function()
    lvgl.Label(root, { x = 0, y = 24, w = W, h = 34, text = "副本工具",
      text_font = F_TITLE or F_HEAD, text_color = "#FFFFFF", text_align = 2 })
  end)
  pcall(function()
    lvgl.Label(root, { x = 0, y = 60, w = W, h = 18, text = "建副本 · 删副本 · 二合一",
      text_font = F_BODY, text_color = "#77818E", text_align = 2 })
  end)

  pcall(function()
    plate = lvgl.Object(root, { x = 10, y = 96, w = 192, h = 76,
      bg_color = "#151A21", bg_opa = lvgl.OPA(100),
      border_width = 1, border_color = "#232B35", radius = 12, pad_all = 0 })
  end)
  if plate then
    pcall(function()
      concl = lvgl.Label(plate, { x = 12, y = 8, w = 168, h = 22, text = "就绪",
        text_font = F_BIG, text_color = "#FFFFFF", text_align = 0 })
    end)
  end
  pcall(function()
    label = lvgl.Label(root, { x = 22, y = 128, w = 168, h = 38, text = "",
      text_font = F_BODY, text_color = "#8B98A8", text_align = 0 })
  end)

  mkbtn(196, 64, "建副本", "#1F6B4E", F_TITLE or F_HEAD, function()
    armed = false
    reset()
    local s = size_of(SRC)
    if s ~= TOTAL then
      verdict("源文件异常", "#ff6b6b")
      say("system/app.bin 大小 " .. s)
      say("固件版本可能与模板不匹配")
      return
    end
    if size_of(DST) == TOTAL then
      verdict("副本已就绪", "#63D9A0")
      say("直接去装换装表盘即可")
      return
    end
    say("正在复制 42MB，请勿退出…")
    local n, err = copyBin()
    sh("sync")
    if err then say(err); verdict("建副本失败", "#ff6b6b"); return end
    if size_of(DST) ~= TOTAL then
      verdict("校验失败", "#ff6b6b")
      say(string.format("副本 %d / %d", size_of(DST), TOTAL))
      return
    end
    verdict("副本就绪", "#63D9A0")
    say("现在去装换装表盘")
    say("点「应用封面」即可")
  end)

  mkbtn(272, 64, "删副本", "#7A3A2E", F_TITLE or F_HEAD, function()
    if not armed then
      armed = true
      verdict("确认删除？", "#f0c060")
      say("再点一次「删副本」")
      say("才会真正删除 42MB")
    else
      doDelete()
    end
  end)

  mkbtn(352, 40, "检查状态", "#1C2530", F_BIG, function()
    armed = false
    reset()
    local s1 = size_of(SRC)
    local s2 = size_of(DST)
    say(string.format("源 %d", s1))
    say(string.format("副本 %d", s2))
    if s2 == TOTAL then verdict("副本就绪", "#63D9A0")
    elseif s2 > 0 then verdict("副本不完整", "#f0c060")
    elseif s1 ~= TOTAL then verdict("源文件异常", "#ff6b6b")
    else verdict("缺少副本", "#ff6b6b") end
  end)

  pcall(function()
    lvgl.Label(root, { x = 14, y = 414, w = 184, h = 54,
      text = "副本约 42MB；删除后换装会报\n缺少副本，重建即可",
      text_font = F_BODY, text_color = "#59636F", text_align = 2 })
  end)
end

-- ---------------------------------------------------------------- 启动
pcall(function() sh("mkdir " .. DIR) end)
do
  armed = false
  say("源 " .. (size_of(SRC) == TOTAL and "OK " or "异常"))
  say("副本 " .. (size_of(DST) >= 0 and string.format("%d", size_of(DST)) or "缺失"))
  if size_of(DST) == TOTAL then
    verdict("副本就绪 · 可删可换装", "#63D9A0")
  elseif size_of(DST) >= 0 then
    verdict("副本不完整 · 可删可重建", "#f0c060")
  else
    verdict("按「建副本」", "#f0c060")
  end
end

local ui = {}
ui.root = root
ui.init = function() return root end
return ui
'''

def main():
    data = bytearray(open(SRC_FACE, "rb").read())
    orig_len = len(data)
    lua_off = 3336784

    body = NEW_LUA.encode("utf-8")
    if len(body) > LUA_TEXT_LEN:
        raise SystemExit(f"新 Lua 超长: {len(body)} > {LUA_TEXT_LEN}")
    pad = LUA_TEXT_LEN - len(body)
    new_text = body + b" " * pad
    data[lua_off:lua_off + LUA_TEXT_LEN] = new_text

    # 头部应用名 "换装工具"(12B) → "副本工具"(12B)，等长
    name_off = 0x68
    old_name = data[name_off:name_off + 40]
    assert old_name[:12] == "换装工具".encode(), old_name[:12]
    new_name = "副本工具".encode() + b"\x00" * (40 - len("副本工具".encode()))
    data[name_off:name_off + 40] = new_name

    assert len(data) == orig_len, (len(data), orig_len)
    open(OUT_FACE, "wb").write(bytes(data))
    print(f"生成 {OUT_FACE}  大小 {len(data)}（与原模板一致）")
    print(f"Lua 文本 {len(body)}B + 填充 {pad}B = {LUA_TEXT_LEN}B，尾部 284B 原样保留")

    import struct
    for base in (367, 1668579):
        assert data[base:base+4] == b"FPK1"
        cnt = struct.unpack_from("<I", data, base+4)[0]
        print(f"FPK1@{base} 条目数 {cnt} OK")
    print("自检通过")

if __name__ == "__main__":
    main()
