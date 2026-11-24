--[[ FPtool 一键换装：应用/还原封面 + 建/删副本（官方深色UI）]]
local lvgl = require("lvgl")
local DIR,SRC,DST = "/data/fpatch","/system/app.bin","/data/fpatch/app.bin"
local TOTAL,W,H = 42667008,212,520
local ENT = {}
do
  local src = "activities|ls200|0|968,f8fbfdff\nalarm|p0t9s|0|1004,fef7ecff\nalipay|3mf74|0|1032,ffffff00\nbreath|5mzbk|0|976,ffe2c2ff\ncalendar|bc9b4|0|956,ffead0ff\nchronograph|bwaxs|0|960,ffd7adff\ncompass|11byo|0|980,cececeff\nfind_phone|cvr1c|0|984,fbce94ff\nflashlight|4onj4|0|944,def1ffff\nheartrate|2oei8|0|1024,bab6ffff\ninterconnect|3t3ww|0|996,ffecd2ff\nlua|b9l8w|2|896,f8f5f5ff\nmedia|boxjk|0|984,d6ccffff\nmijia|vfuo|0|1008,e7f4d2ff\nmore|fj3dc|0|984,ffffff00\nmute_phone|1zn0g|0|1032,ffe6c0ff\nnfccard|1d62o|0|928,ffdfb5ff\nperpetual_calendar|b87fk|0|820,fbfaf8ff\nperpetual_calendar/launcher_icon.bin|b8thc|0|864,fdfdfcff\nphone/contacts_launcher.bin|3ir5s|2|940,ffe6cbff\nphone|3g5eo|2|980,cff3b3ff\nrecorder|ksa00|0|964,d9d9d9ff\nremote_camera|fjdy8|0|96,faf9f76f,020101ff\nsettings|29h7k|0|1024,f9f2ecff\nsleep|20d9c|0|1020,fdf0f2ff\noxygen|5bm0w|0|1032,c0bafeff\nsports|gbm9s|0|736,0a0b0bff,e0ffff20\nsports/course|gmo28|0|1012,feeaeaff\nsports/record|glzy8|0|1028,9bc0feff\nsports/training|fuw9s|0|1012,f8fef8ff\npressure|4h24g|0|1008,fafdf1ff\ntimer|1vnm8|0|976,fcbb73ff\ntodolist|fe2xs|0|976,abdefbff\nvoice/aivs|abkw|0|952,c2e5e8ff\nvoice/alexa|e5k0|2|248,f6edd1ff\nweather|d1vbk|0|968,cee8eaff\nwomenhealth|p5y74|0|976,dbceffff\nwxpay|3uugw|0|60,00af1010,ebf7dfff\nnfccard/access_card_01.bin|1i4lc|4|24372,0309180b,94f0f1f2\nnfccard/access_card_02.bin|1dglc|4|24372,09051712,94f0f1f2\nnfccard/access_card_03.bin|1eikg|6|57060,f0f0f0f0,06060623\nnfccard/access_card_04.bin|1gbkw|6|36556,fdfdfdfd,070a0a0a\nnfccard/access_card_sdoor.bin|1s5ps|3|2776,ebe5ebe5,04040404\nnfccard/access_card_work.bin|1amgg|4|5068,10100707,d0e4e4d0\nnfccard/bank_card_mastercard.bin|1tnls|7|22024,ffececec,08080808\nnfccard/car_card_biyadi.bin|1q12o|8|43120,0d1500ff,fafffeff\nnfccard/car_card_bmw.bin|1o0fk|8|90168,00000000,f7f6ed00\nnfccard/car_card_brnd.bin|1lgsw|4|12112,e6e6e6db,02020202\nnfccard/car_card_fangchengbao.bin|1b5g0|8|78492,1b1613ff,ffffffff\nnfccard/car_card_iccoa.bin|16efk|4|2784,faeefcfb,18051818\nnfccard/car_card_lot1.bin|13hj4|4|6296,fad2d2fa,01010101\nnfccard/car_card_mini.bin|17380|8|90168,00000000,f7f6ed00\nnfccard/car_card_motor.bin|1inkw|5|728,ffffff00,06170dff\nnfccard/car_card_tengshi.bin|1jc4g|8|85604,341c10ff,ffffffff\nnfccard/car_card_xia1.bin|1ssls|1|15344,d2e9e9e9,06060606\nnfccard/car_card_xiao.bin|1dzkw|4|676,ffffff00,24431def\nnfccard/car_card_yangwang.bin|1lzsg|8|85604,16100eff,ffffffff\nnfccard/car_card_yu7.bin|193v4|4|676,ffffff00,24431def\nnfccard/ecc.bin|1t83k|1|36,ffffff1f,344b20ff"
  local LV={13580,20044,21772,24436,24556,31756,84204,93196,94092}
  local function b36(t) local n=0 for i=1,#t do local d=t:byte(i); n=n*36+(d<58 and d-48 or d-87) end return n end
  for line in src:gmatch("[^\n]+") do
    local path,off,li,smp = line:match("([^|]+)|([^|]+)|([^|]+)|(.+)")
    if not path:find("%.") then path=path.."/launcher.bin" end
    local a,b,c = smp:match("([^,]+),([^,]+),?(.*)")
    if c=="" then c="00000000" end
    ENT[#ENT+1] = { path, b36(off), LV[tonumber(li)+1], { {tonumber(a), b, c} } }
  end
end
local function sh(cmd)
  local a,b,c = os.execute(cmd)
  if type(a)=="number" then return a end
  if a==true then return 0 end
  if type(c)=="number" then return c end
  if type(b)=="number" then return b end
  return -1
end
local function code(rc)
  if rc<0 then return rc end
  if rc>=256 then return rc//256 end
  return rc
end
local function size_of(p)
  local f=io.open(p,"rb")
  if not f then return -1 end
  local n=f:seek("end") or -1
  f:close()
  return n
end
local function hex2bin(h)
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
local root,label,plate,concl,F_BODY,F_HEAD,F_BIG,F_TITLE
pcall(function()
  F_BODY=lvgl.Font("MiSans-Regular",11)
  F_HEAD=lvgl.Font("MiSans-Regular",16)
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
local function mkbtn(y,h,x,w,text,fg,cb,fs,p)
  local ok,obj=pcall(function()
    return lvgl.Object(p or root,{x=x,y=y,w=w,h=h,bg_color="#1E1E1E",bg_opa=lvgl.OPA(100),border_width=1,border_color="#303030",radius=16,pad_all=0})
  end)
  if ok and obj then
    pcall(function() obj:clear_flag(lvgl.FLAG.SCROLLABLE) end)
    pcall(function() obj:add_flag(lvgl.FLAG.CLICKABLE) end)
    pcall(function() obj:add_flag(lvgl.FLAG.EVENT_BUBBLE) end)
    pcall(function()
      local lh=fs and 22 or 28
      local lb=lvgl.Label(obj,{x=0,y=(h-lh)//2,w=w,h=lh,text=text,text_font=fs or F_TITLE or F_HEAD,text_color=fg,text_align=2})
      lb:clear_flag(lvgl.FLAG.CLICKABLE)
      lb:add_flag(lvgl.FLAG.EVENT_BUBBLE)
    end)
    pcall(function() obj:onevent(lvgl.EVENT.CLICKED,function() pcall(cb) end) end)
  end
  return obj
end
local fpkPath,restPath
local function dirOf(p)
  if p:sub(-1)~="/" then p=p.."/" end
  p=p:gsub("[^/]+%.lua/$","")
  return p
end
local function findFile(name)
  local list={}
  local sp=nil
  pcall(function() sp=SCRIPT_PATH end)
  if type(sp)=="string" and #sp>0 then
    local b=dirOf(sp)
    list[#list+1]=b..name
    list[#list+1]=b.."../"..name
    list[#list+1]=b.."../../"..name
  end
  for i=1,#list do
    if size_of(list[i])>8 then return list[i] end
  end
  return nil
end
local function fpkCount(p)
  local f=io.open(p,"rb")
  if not f then return nil end
  if f:read(4)~="FPK1" then f:close() return nil end
  local hh=f:read(4)
  f:close()
  return u16(hh,1)
end
local function applyFpk(p,target)
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
local function probe(path,samples)
  local f=io.open(path,"rb")
  if not f then return nil end
  local n,o,x=0,0,0
  for i=1,#samples do
    local s=samples[i]
    f:seek("set",s[1])
    local got=f:read(4)
    if got==hex2bin(s[3]) then n=n+1
    elseif got==hex2bin(s[2]) then o=o+1
    else x=x+1 end
  end
  f:close()
  return n,o,x
end
local function scanAll()
  local n,o,x=0,0,0
  for i=1,#ENT do
    local e=ENT[i]
    local a,b,c=probe("/resource/app/"..e[1],e[4])
    if a==nil then x=x+1
    else
      if a>=#e[4] then n=n+1
      elseif b>=#e[4] then o=o+1
      else x=x+1 end
    end
  end
  return n,o,x
end
local function mountTo(path)
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
local function copyBin()
  local fin=io.open(SRC,"rb")
  if not fin then return -1,"打不开源文件" end
  local fout=io.open(DST,"wb")
  if not fout then fin:close() return -2,"写入失败 存储不足" end
  local total,blocks=0,0
  local ok,err=pcall(function()
    while true do
      local blk=fin:read(65536)
      if not blk or #blk==0 then break end
      fout:write(blk)
      total=total+#blk
      blocks=blocks+1
      if blocks%96==0 then
        say(string.format("已复制 %d%%",total//426671))
      end
    end
  end)
  fin:close()
  fout:close()
  if not ok then return total,"复制中断："..tostring(err or "空间不足") end
  return total,nil
end
local armed=false
local about=nil
if root then
  pcall(function()
    lvgl.Label(root,{x=0,y=14,w=W,h=34,text="换装工具",text_font=F_TITLE,text_color="#FFFFFF",text_align=2})
  end)
  pcall(function()
    lvgl.Label(root,{x=0,y=50,w=W,h=18,text="图标·封面·副本",text_font=F_BODY,text_color="#8A8A8A",text_align=2})
  end)
  pcall(function()
    plate=lvgl.Object(root,{x=10,y=74,w=192,h=64,bg_color="#1E1E1E",bg_opa=lvgl.OPA(100),border_width=1,border_color="#303030",radius=14,pad_all=0})
  end)
  if plate then
    pcall(function()
      concl=lvgl.Label(plate,{x=14,y=20,w=164,h=24,text="就绪",text_font=F_BIG,text_color="#FFFFFF",text_align=0})
    end)
  end
  pcall(function()
    label=lvgl.Label(root,{x=24,y=142,w=164,h=48,text="",text_font=F_BODY,text_color="#7A7A7A",text_align=0})
  end)
  mkbtn(198,60,10,192,"应用封面","#FFFFFF",function()
    reset()
    if size_of(DST)~=TOTAL then
      verdict("缺少副本","#ff7a7a")
      say("先点建副本")
      return
    end
    if not fpkPath then verdict("缺少数据","#ff7a7a") return end
    say("正在写入…")
    local done,_,err=applyFpk(fpkPath,DST)
    if err then say(err) verdict("写入失败","#ff7a7a") return end
    sh("sync")
    local r3,r4=mountTo(DST)
    if r4~=0 then
      mountTo(SRC)
      verdict("挂载失败","#ff7a7a")
      say(string.format("L%d M%d 已写入",r3,r4))
      say("重启后再试")
      return
    end
    scanAll()
    verdict(string.format("已写 %d 条",done),"#63D9A0")
    say("切表盘即可生效")
  end)
  mkbtn(266,60,10,192,"还原原厂","#FFFFFF",function()
    reset()
    if size_of(DST)~=TOTAL then
      verdict("缺少副本","#ff7a7a")
      say("先点建副本")
      return
    end
    if not restPath then verdict("缺少数据","#ff7a7a") return end
    say("正在还原…")
    local done,_,err=applyFpk(restPath,DST)
    if err then say(err) verdict("写入失败","#ff7a7a") return end
    sh("sync")
    local r3,r4=mountTo(DST)
    if r4~=0 then
      mountTo(SRC)
      verdict("挂载失败","#ff7a7a")
      say(string.format("L%d M%d 已写入",r3,r4))
      say("重启后再试")
      return
    end
    scanAll()
    verdict(string.format("已还原 %d 条",done),"#f0c060")
    say("已回到原厂图标")
  end)
  mkbtn(334,50,10,92,"建副本","#63D9A0",function()
    reset()
    local s=size_of(SRC)
    if s~=TOTAL then
      verdict("源异常","#ff7a7a")
      say("大小 "..s)
      return
    end
    if size_of(DST)==TOTAL then
      verdict("副本已就绪","#63D9A0")
      say("直接应用封面即可")
      return
    end
    say("复制42MB 勿退出")
    local n,err=copyBin()
    sh("sync")
    if err then say(err) verdict("建副本失败","#ff7a7a") return end
    if size_of(DST)~=TOTAL then
      verdict("校验失败","#ff7a7a")
      say(string.format("副本 %d / %d",size_of(DST),TOTAL))
      return
    end
    verdict("副本就绪","#63D9A0")
    say("现在可应用封面")
  end)
  mkbtn(334,50,110,92,"删副本","#FF9B9B",function()
    reset()
    if size_of(DST)<0 then
      verdict("本无副本","#63D9A0")
      say("空间干净")
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
    sh("umount /resource/app")
    sh("umount -l /resource/app")
    sh("losetup -d /dev/loop0")
    pcall(function() os.remove(DST) end)
    sh("sync")
    local rr=mountTo(SRC)
    if size_of(DST)<0 then
      verdict("已删除","#63D9A0")
      if rr==0 then
        say("释放42MB 资源已还原")
      else
        say("释放42MB 重启恢复图标")
      end
    else
      verdict("删除失败","#ff7a7a")
      say("被占用 重启再试")
    end
  end)
  mkbtn(392,40,10,92,"体检状态","#B8B8B8",function()
    reset()
    local n,o,x=scanAll()
    say(string.format("新 %d · 旧 %d · 其他 %d",n,o,x))
    if size_of(DST)~=TOTAL then verdict("缺少副本","#ff7a7a")
    elseif n==#ENT then verdict("全部已换","#63D9A0")
    else verdict("待应用","#f0c060") end
  end,F_BIG)
  mkbtn(392,40,110,92,"关于","#B8B8B8",function()
    if about then pcall(function() about:set{x=0,y=0} end) end
  end,F_BIG)
  pcall(function()
    lvgl.Label(root,{x=14,y=448,w=184,h=54,text="切表盘生效\n重启可恢复",text_font=F_BODY,text_color="#5A5A5A",text_align=2})
  end)
  local AV1,AV2
  pcall(function()
    local sp
    pcall(function() sp=SCRIPT_PATH end)
    if type(sp)~="string" or #sp==0 then return end
    local f=io.open(sp,"rb"); if not f then return end
    local z=f:seek("end")
    if not z or z<560 then f:close() return end
    f:seek("set",z-556)
    AV1=f:read(276)
    AV2=f:read(276)
    f:close()
  end)
  pcall(function()
    about=lvgl.Object(root,{x=0,y=520,w=W,h=H,bg_color="#000000",bg_opa=lvgl.OPA(100),border_width=0,radius=0,pad_all=0})
    about:clear_flag(lvgl.FLAG.SCROLLABLE)
    lvgl.Label(about,{x=0,y=14,w=W,h=34,text="关于",text_font=F_TITLE,text_color="#FFFFFF",text_align=2})
    lvgl.Label(about,{x=14,y=66,w=180,h=28,text="Developers",text_font=F_HEAD,text_color="#8A8A8A",text_align=0})
    for i,nm in ipairs{"DieNuo","Qq"} do
      local y=112+(i-1)*80
      local px=(i==1 and AV1 or AV2)
      local shown=false
      if px and px:byte(1)==0x19 then
        pcall(function()
          local im=lvgl.Img or lvgl.Image or lvgl.img
          if im then im(about,{x=26,y=y,w=20,h=20,src=px}) shown=true end
        end)
      end
      if not shown then
        local c=lvgl.Object(about,{x=26,y=y,w=20,h=20,bg_color="#1E1E1E",bg_opa=lvgl.OPA(100),border_width=1,border_color="#303030",radius=12,pad_all=0})
        lvgl.Label(c,{x=0,y=0,w=20,h=20,text=nm:sub(1,1),text_font=F_HEAD,text_color="#FFFFFF",text_align=2})
      end
      lvgl.Label(about,{x=52,y=y,w=140,h=24,text=nm,text_font=F_HEAD,text_color="#FFFFFF",text_align=0})
    end
    lvgl.Label(about,{x=0,y=290,w=W,h=18,text="感谢米坛社区与每位反馈的朋友",text_font=F_BODY,text_color="#7A7A7A",text_align=2})
    lvgl.Label(about,{x=0,y=316,w=W,h=18,text="FPtool v2.2.6",text_font=F_BODY,text_color="#5A5A5A",text_align=2})
    mkbtn(440,50,10,192,"返回","#FFFFFF",function()
      pcall(function() about:set{x=0,y=520} end)
    end,nil,about)
  end)
end
pcall(function() sh("mkdir "..DIR) end)
fpkPath=findFile("all.fpk")
restPath=findFile("all_restore.fpk")
do
  say("数据包 "..(fpkPath and (tostring(fpkCount(fpkPath)).." 条") or "缺失"))
  say("还原包 "..(restPath and "OK" or "缺失"))
  say("副本 "..(size_of(DST)==TOTAL and "就绪" or "缺失"))
  local n,o,x=scanAll()
  say(string.format("当前 新%d 旧%d 其他%d",n,o,x))
  if size_of(DST)~=TOTAL then
    verdict("先建副本再应用","#f0c060")
  elseif n==#ENT then
    verdict("全部已换，切表盘看","#63D9A0")
  else
    verdict("按「应用封面」","#f0c060")
  end
end
local ui={}
ui.root=root
ui.init=function() return root end
return ui