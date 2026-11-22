--[[ FPtool 控制中心]]
local lvgl = require("lvgl")
local DIR,SRC,DST = "/data/fpatch","/system/app.bin","/data/fpatch/app.bin"
local TOTAL,W,H = 42667008,212,520

local ENT = {
{ "control_center/icon/not_disturb_close.bin",4205968,11036,{{288,"ffffff00","4b4b4bef"},{1032,"ffffff00","00000000"}} },
{ "control_center/icon/not_disturb_open.bin",4328448,11036,{{1032,"ffffff00","00000000"},{1088,"0b040606","c1cacbcb"}} },
{ "control_center/icon/find_phone_close.bin",4120272,11036,{{28,"2121211f","fff1ddff"},{1196,"0a141103","d9d7bdda"}} },
{ "control_center/icon/flashlights.bin",4194880,11036,{{16,"20202010","f3fbffff"},{96,"25252530","f4fcffff"}} },
{ "control_center/icon/settings.bin",4102352,11036,{{432,"ffffff00","211a18ff"},{736,"ffffff00","1e1816ff"}} },
}

local function sh(c)
  local a,b,d = os.execute(c)
  if type(a)=="number" then return a end
  if a==true then return 0 end
  if type(d)=="number" then return d end
  if type(b)=="number" then return b end
  return -1
end
local function code(rc)
  if rc<0 then return rc end
  if rc>=256 then return rc//256 end
  return rc
end
local function sz(p)
  local f=io.open(p,"rb")
  if not f then return -1 end
  local n=f:seek("end") or -1
  f:close()
  return n
end
local function h2b(h)
  return (h:gsub("%x%x",function(cc) return string.char(tonumber(cc,16)) end))
end
local function u16(s,i)
  local a,b=s:byte(i,i+1)
  if not b then return nil end
  return a+b*256
end
local function u32(s,i)
  local a,b,c,d=s:byte(i,i+3)
  if not d then return nil end
  return a+b*256+c*65536+d*16777216
end

local root,label,plate,concl,F_BODY,F_BIG,F_TITLE
pcall(function()
  F_BODY=lvgl.Font("MiSans-Regular",11)
  F_BIG=lvgl.Font("MiSans-Regular",15)
  F_TITLE=lvgl.Font("MiSans-Regular",20)
end)
pcall(function()
  root=lvgl.Object(nil,{x=0,y=0,w=W,h=H,bg_color="#000000",bg_opa=lvgl.OPA(100),border_width=0,radius=0,pad_all=0})
  root:clear_flag(lvgl.FLAG.SCROLLABLE)
end)

local buf={}
local function say(s)
  buf[#buf+1]=tostring(s)
  while #buf>3 do table.remove(buf,1) end
  if label then pcall(function() label:set{text=table.concat(buf,"\n")} end) end
end
local function reset()
  buf={}
  if label then pcall(function() label:set{text=""} end) end
end
local function verdict(t,c)
  if concl then pcall(function() concl:set{text=t,text_color=c} end) end
  if plate then
    local bg=0x1E1E1E
    if c=="#63D9A0" then bg=0x143226
    elseif c=="#ff7a7a" then bg=0x361818
    elseif c=="#f0c060" then bg=0x362b14 end
    pcall(function() plate:set{bg_color=bg} end)
  end
end
local function mkbtn(y,h,x,w,text,fg,cb,fs)
  local ok,obj=pcall(function()
    return lvgl.Object(root,{x=x,y=y,w=w,h=h,bg_color="#1E1E1E",bg_opa=lvgl.OPA(100),border_width=1,border_color="#303030",radius=16,pad_all=0})
  end)
  if ok and obj then
    pcall(function() obj:clear_flag(lvgl.FLAG.SCROLLABLE) end)
    pcall(function() obj:add_flag(lvgl.FLAG.CLICKABLE) end)
    pcall(function() obj:add_flag(lvgl.FLAG.EVENT_BUBBLE) end)
    pcall(function()
      local lh=fs and 22 or 28
      local lb=lvgl.Label(obj,{x=0,y=(h-lh)//2,w=w,h=lh,text=text,text_font=fs or F_TITLE,text_color=fg,text_align=2})
      lb:clear_flag(lvgl.FLAG.CLICKABLE)
      lb:add_flag(lvgl.FLAG.EVENT_BUBBLE)
    end)
    pcall(function() obj:onevent(lvgl.EVENT.CLICKED,function() pcall(cb) end) end)
  end
  return obj
end

local fpath,rpath
local function findFile(name)
  local list={}
  local sp=nil
  pcall(function() sp=SCRIPT_PATH end)
  if type(sp)=="string" and #sp>0 then
    local b=sp
    if b:sub(-1)~="/" then b=b.."/" end
    b=b:gsub("[^/]+%.lua/$","")
    list[#list+1]=b..name
    list[#list+1]=b.."../"..name
    list[#list+1]=b.."../../"..name
  end
  for i=1,#list do
    if sz(list[i])>8 then return list[i] end
  end
  return nil
end
local function fcnt(p)
  local f=io.open(p,"rb")
  if not f then return nil end
  if f:read(4)~="FPK1" then f:close() return nil end
  local hh=f:read(4)
  f:close()
  return u16(hh,1)
end

local function af(p,target)
  local f=io.open(p,"rb")
  if not f then return 0,0,"打不开fpk" end
  if f:read(4)~="FPK1" then f:close() return 0,0,"magic不符" end
  local hh=f:read(4)
  local count=u16(hh,1)
  if not count then f:close() return 0,0,"头部坏" end
  local out=io.open(target,"r+b")
  if not out then f:close() return 0,0,"目标打不开" end
  local done,bad=0,0
  for i=1,count do
    local hdr=f:read(8)
    if not hdr or #hdr<8 then bad=bad+1 break end
    local off=u32(hdr,1)
    local len=u32(hdr,5)
    local data=f:read(len)
    if not data or #data~=len then bad=bad+1 break end
    out:seek("set",off)
    if out:write(data) then done=done+1 else bad=bad+1 end
  end
  out:close()
  f:close()
  return done,bad,nil
end

local function pb(path,samples)
  local f=io.open(path,"rb")
  if not f then return nil end
  local n,o,x=0,0,0
  for i=1,#samples do
    local s=samples[i]
    f:seek("set",s[1])
    local got=f:read(4)
    if got==h2b(s[3]) then n=n+1
    elseif got==h2b(s[2]) then o=o+1
    else x=x+1 end
  end
  f:close()
  return n,o,x
end

local function sn()
  local n,o,x=0,0,0
  for i=1,#ENT do
    local e=ENT[i]
    local a,b,c=pb("/resource/app/"..e[1],e[4])
    if a==nil then x=x+1
    else
      if a>=#e[4] then n=n+1
      elseif b>=#e[4] then o=o+1
      else x=x+1 end
    end
  end
  return n,o,x
end

local function mnt(path)
  sh("umount /resource/app") sh("losetup -d /dev/loop0")
  local r3=code(sh("losetup -r /dev/loop0 "..path))
  local r4=code(sh("mount -t romfs /dev/loop0 /resource/app"))
  if r4~=0 then
    sh("umount -l /resource/app") sh("losetup -d /dev/loop0")
    r3=code(sh("losetup -r /dev/loop0 "..path))
    r4=code(sh("mount -t romfs /dev/loop0 /resource/app"))
  end
  return r3,r4
end

local function cp()
  local fin=io.open(SRC,"rb")
  if not fin then return -1,"打不开源文件" end
  local fout=io.open(DST,"wb")
  if not fout then fin:close() return -2,"写入失败 空间不足" end
  local total=0
  local ok,err=pcall(function()
    while true do
      local blk=fin:read(65536)
      if not blk or #blk==0 then break end
      fout:write(blk)
      total=total+#blk
    end
  end)
  fin:close()
  fout:close()
  if not ok then return total,"复制中断："..tostring(err or "空间不足") end
  return total,nil
end

local armed=false
if root then
  pcall(function()
    lvgl.Label(root,{x=0,y=16,w=W,h=30,text="控制中心换装",text_font=F_TITLE or F_HEAD,text_color="#FFFFFF",text_align=2})
  end)
  pcall(function()
    lvgl.Label(root,{x=0,y=50,w=W,h=18,text="图标·副本",text_font=F_BODY,text_color="#8A8A8A",text_align=2})
  end)
  pcall(function()
    plate=lvgl.Object(root,{x=10,y=72,w=192,h=64,bg_color="#1E1E1E",bg_opa=lvgl.OPA(100),border_width=1,border_color="#303030",radius=14,pad_all=0})
  end)
  if plate then
    pcall(function()
      concl=lvgl.Label(plate,{x=14,y=20,w=164,h=24,text="就绪",text_font=F_BIG,text_color="#FFFFFF",text_align=0})
    end)
  end
  pcall(function()
    label=lvgl.Label(root,{x=24,y=142,w=164,h=48,text="",text_font=F_BODY,text_color="#7A7A7A",text_align=0})
  end)

  mkbtn(198,60,10,192,"应用图标","#FFFFFF",function()
    reset()
    if sz(DST)~=TOTAL then
      verdict("缺少副本","#ff7a7a")
      say("先建副本")
      return
    end
    if not fpath then verdict("缺少数据","#ff7a7a") return end
    say("正在写入…")
    local done,_,err=af(fpath,DST)
    if err then say(err) verdict("写入失败","#ff7a7a") return end
    sh("sync")
    local r3,r4=mnt(DST)
    if r4~=0 then verdict("挂载失败","#ff7a7a") say(string.format("L%d M%d 已写入",r3,r4)) say("重启后再试") return end
    sn()
    verdict(string.format("已写 %d 条",done),"#63D9A0")
    say("切表盘即可生效")
  end)

  mkbtn(266,60,10,192,"还原原厂","#FFFFFF",function()
    reset()
    if sz(DST)~=TOTAL then
      verdict("缺少副本","#ff7a7a")
      say("先建副本")
      return
    end
    if not rpath then verdict("缺少数据","#ff7a7a") return end
    say("正在还原…")
    local done,_,err=af(rpath,DST)
    if err then say(err) verdict("写入失败","#ff7a7a") return end
    sh("sync")
    local r3,r4=mnt(DST)
    if r4~=0 then verdict("挂载失败","#ff7a7a") say(string.format("L%d M%d 已写入",r3,r4)) say("重启后再试") return end
    sn()
    verdict(string.format("已还原 %d 条",done),"#f0c060")
    say("已还原原厂")
  end)

  mkbtn(334,50,10,92,"建副本","#63D9A0",function()
    reset()
    if sz(SRC)~=TOTAL then
      verdict("源文件异常","#ff7a7a")
      say("大小 "..sz(SRC))
      return
    end
    if sz(DST)==TOTAL then
      verdict("副本已就绪","#63D9A0")
      say("直接应用图标")
      return
    end
    say("复制中 勿退出")
    local n,err=cp()
    sh("sync")
    if err then say(err) verdict("建副本失败","#ff7a7a") return end
    if sz(DST)~=TOTAL then
      verdict("校验失败","#ff7a7a")
      say(string.format("副本 %d / %d",sz(DST),TOTAL))
      return
    end
    verdict("副本就绪","#63D9A0")
    say("可应用了")
  end)

  mkbtn(334,50,110,92,"删副本","#FF9B9B",function()
    reset()
    if sz(DST)<0 then
      verdict("本来就没有副本","#63D9A0")
      say("空间已干净")
      armed=false
      return
    end
    if not armed then
      armed=true
      verdict("确认删除？","#f0c060")
      say("再点一次删副本")
      return
    end
    armed=false
    say("正在删除…")
    sh("umount -l /resource/app")
    sh("losetup -d /dev/loop0")
    pcall(function() os.remove(DST) end)
    sh("sync")
    if sz(DST)<0 then
      verdict("已删除","#63D9A0")
      say("释放 42MB")
    else
      verdict("删除失败","#ff7a7a")
      say("占用 重启再试")
    end
  end)

  mkbtn(392,40,10,192,"体检状态","#B8B8B8",function()
    reset()
    local n,o,x=sn()
    say(string.format("新%d 旧%d 其他%d",n,o,x))
    if sz(DST)~=TOTAL then verdict("缺少副本","#ff7a7a")
    elseif n==#ENT then verdict("全部已换","#63D9A0")
    else verdict("待应用","#f0c060") end
  end,F_BIG)

  pcall(function()
    lvgl.Label(root,{x=14,y=448,w=184,h=40,text="应用后切表盘生效",text_font=F_BODY,text_color="#5A5A5A",text_align=2})
  end)
end

pcall(function() sh("mkdir "..DIR) end)
fpath=findFile("cc.fpk")
rpath=findFile("cc_restore.fpk")

do
  say("数据包 "..(fpath and (tostring(fcnt(fpath)).." 条") or "缺失"))
  say("还原包 "..(rpath and "OK" or "缺失"))
  say("副本 "..(sz(DST)==TOTAL and "就绪" or "缺失"))
  local n,o,x=sn()
  say(string.format("新%d 旧%d 其他%d",n,o,x))
  if sz(DST)~=TOTAL then
    verdict("先建副本","#f0c060")
  elseif n==#ENT then
    verdict("全部已换","#63D9A0")
  else
    verdict("按应用图标","#f0c060")
  end
end

local ui = {}
ui.root = root
ui.init = function() return root end
return ui
