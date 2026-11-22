--[[ FPtool 一键换装：应用/还原封面 + 建/删副本（官方深色UI）]]
local lvgl = require("lvgl")
local DIR,SRC,DST = "/data/fpatch","/system/app.bin","/data/fpatch/app.bin"
local TOTAL,W,H = 42667008,212,520
local ENT = {}
do
  local src = "activities/launcher.bin|36580896|13580|968,f8fbfdff,00000000;7908,e7f4f4f4,01010101\nalarm/launcher.bin|42028336|13580|1004,fef7ecff,00000000;4788,cbcbcbcb,08080808\nalipay/launcher.bin|6084976|13580|1032,ffffff00,00000000;12856,02160511,e3e0e499\nbreath/launcher.bin|9470288|13580|976,ffe2c2ff,00000000;12844,0c050e0e,bcdfe8e8\ncalendar/launcher.bin|19047712|13580|956,ffead0ff,00000000;4424,e5e9bef4,0a070a0a\nchronograph/launcher.bin|19982944|13580|960,ffd7adff,00000000;1016,ffd4b5ff,00000000\ncompass/launcher.bin|1741776|13580|980,cececeff,00000000;1032,e7e7e7ff,00000000\nfind_phone/launcher.bin|21636768|13580|984,fbce94ff,00000000;1768,1717120f,dcdcd2d1\nflashlight/launcher.bin|7868704|13580|944,def1ffff,00000000;1004,edf7feff,00000000\nheartrate/launcher.bin|4497776|13580|1024,bab6ffff,00000000;1984,f8f8f8f8,24242424\ninterconnect/launcher.bin|6396944|13580|996,ffecd2ff,00000000;11060,fbcfcfe9,07070505\nlua/launcher.bin|18923216|21772|896,f8f5f5ff,00000000;996,fbfbfbff,00000000\nmedia/launcher.bin|19638992|13580|984,d6ccffff,00000000;1984,eeeeeeee,12121212\nmijia/launcher.bin|1466880|13580|1008,e7f4d2ff,00000000;8240,c8c8c8c7,02020202\nmore/launcher.bin|26085072|13580|984,ffffff00,00000000;1032,ffffff00,00000000\nmute_phone/launcher.bin|3342400|13580|1032,ffe6c0ff,00000000;10388,f5e1f6f1,2c050505\nnfccard/launcher.bin|2294016|13580|928,ffdfb5ff,00000000;992,dfd0b0ff,00000000\nperpetual_calendar/launcher.bin|18858656|13580|820,fbfaf8ff,00000000;1000,faf0e8ff,00000000\nperpetual_calendar/launcher_icon.bin|18887232|13580|864,fdfdfcff,00000000;980,fefdfcfe,00000000\nphone/contacts_launcher.bin|5913856|21772|940,ffe6cbff,00000000;1000,ffedd3ff,00000000\nphone/launcher.bin|5792352|21772|980,cff3b3ff,00000000;1028,84db45ff,00000000\nrecorder/launcher.bin|34911648|13580|964,d9d9d9ff,00000000;9148,c9e3eef7,2c3a0246\nremote_camera/launcher.bin|26098784|13580|96,faf9f76f,020101ff;956,e3e2e1ff,00000000\nsettings/launcher.bin|3801440|13580|1024,f9f2ecff,00000000;3216,9797eaf3,06060606\nsleep/launcher.bin|3376416|13580|1020,fdf0f2ff,00000000;4664,e3efeee5,02021e03\noxygen/launcher.bin|8939840|13580|1032,c0bafeff,00000000;11180,0d0d0d64,f6dedd04\nsports/launcher.bin|27415936|13580|736,0a0b0bff,e0ffff20;916,0a1617ff,e7ffff70\nsports/course/launcher.bin|27931472|13580|1012,feeaeaff,00000000;4764,dcdcdcdc,05050505\nsports/record/launcher.bin|27900224|13580|1028,9bc0feff,00000000;10704,dfeeeedf,04340434\nsports/training/launcher.bin|26635744|13580|1012,f8fef8ff,00000000;6532,d0d0d0d0,0d0d0d0d\npressure/launcher.bin|7514368|13580|1008,fafdf1ff,00000000;7252,d4c1d3bb,12121215\ntimer/launcher.bin|3156560|13580|976,fcbb73ff,00000000;1028,fef7f4ff,00000000\ntodolist/launcher.bin|25851232|13580|976,abdefbff,00000000;1032,fbfbfdff,00000000\nvoice/aivs/launcher.bin|481568|13580|952,c2e5e8ff,00000000;10140,dfc1c1df,01010101\nvoice/alexa/launcher.bin|660384|21772|248,f6edd1ff,00000000;1032,ffffff00,00000000\nweather/launcher.bin|21922256|13580|968,cee8eaff,00000000;1016,f7d099ff,00000000\nwomenhealth/launcher.bin|42268000|13580|976,dbceffff,00000000;1024,fdfcffff,00000000\nwxpay/launcher.bin|6478016|13580|60,00af1010,ebf7dfff;1032,ffffff00,00000000\nnfccard/access_card_01.bin|2525376|24556|24372,0309180b,94f0f1f2;24540,0b0e0912,f5f6f7f8\nnfccard/access_card_02.bin|2307648|24556|24372,09051712,94f0f1f2;24540,06100519,f5f6f7f8\nnfccard/access_card_03.bin|2356864|84204|57060,f0f0f0f0,06060623;81456,f5f5f5f5,46020202\nnfccard/access_card_04.bin|2441120|84204|36556,fdfdfdfd,070a0a0a;65972,fafafafa,02020202\nnfccard/access_card_sdoor.bin|2993392|24436|2776,ebe5ebe5,04040404;2972,fae4fbf9,04040404\nnfccard/access_card_work.bin|2175280|24556|5068,10100707,d0e4e4d0;19440,d8d8f6f4,060b0606\nnfccard/bank_card_mastercard.bin|3063232|93196|22024,ffececec,08080808;81008,03060306,eaefddcb\nnfccard/car_card_biyadi.bin|2894064|94092|43120,0d1500ff,fafffeff;90168,00000000,f7f6ed00\nnfccard/car_card_bmw.bin|2799920|94092|90168,00000000,f7f6ed00;94052,00000000,d9e1da00\nnfccard/car_card_brnd.bin|2681168|24556|12112,e6e6e6db,02020202;14780,e2f2f4fc,0f0f0f0f\nnfccard/car_card_fangchengbao.bin|2199888|94092|78492,1b1613ff,ffffffff;90168,00000000,f7f6ed00\nnfccard/car_card_iccoa.bin|1978256|24556|2784,faeefcfb,18051818;4344,f4fcfcfc,1f180505\nnfccard/car_card_lot1.bin|1842304|24556|6296,fad2d2fa,01010101;7480,fbf5fdfd,03031338\nnfccard/car_card_mini.bin|2010384|94092|90168,00000000,f7f6ed00;94052,00000000,d9e1da00\nnfccard/car_card_motor.bin|2549984|31756|728,ffffff00,06170dff;928,ffffff00,193116ff\nnfccard/car_card_tengshi.bin|2581792|94092|85604,341c10ff,ffffffff;90168,00000000,f7f6ed00\nnfccard/car_card_xia1.bin|3023056|20044|15344,d2e9e9e9,06060606;16792,d9d9eaea,02020404\nnfccard/car_card_xiao.bin|2332256|24556|676,ffffff00,24431def;24540,1d0a1312,f4f5f6f7\nnfccard/car_card_yangwang.bin|2705776|94092|85604,16100eff,ffffffff;90168,00000000,f7f6ed00\nnfccard/car_card_yu7.bin|2104528|24556|676,ffffff00,24431def;24540,1a101514,f4f5f6f7\nnfccard/ecc.bin|3043136|20044|36,ffffff1f,344b20ff;1016,f1e6fcff,00000000"
  for line in src:gmatch("[^\n]+") do
    local path, off, ln, smp = line:match("([^|]+)|([^|]+)|([^|]+)|(.+)")
    local s = {}
    for a, b, c in smp:gmatch("([^,;]+),([^,;]+),([^,;]+)") do
      s[#s + 1] = { tonumber(a), b, c }
    end
    ENT[#ENT + 1] = { path, tonumber(off), tonumber(ln), s }
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
if root then
  pcall(function()
    lvgl.Label(root,{x=0,y=14,w=W,h=34,text="换装工具",text_font=F_TITLE,text_color="#FFFFFF",text_align=2})
  end)
  pcall(function()
    lvgl.Label(root,{x=0,y=50,w=W,h=18,text="图标 · 封面 · 副本管理",text_font=F_BODY,text_color="#8A8A8A",text_align=2})
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
      say("先点下方「建副本」")
      return
    end
    if not fpkPath then verdict("缺少数据","#ff7a7a") return end
    say("正在写入…")
    local done,_,err=applyFpk(fpkPath,DST)
    if err then say(err) verdict("写入失败","#ff7a7a") return end
    sh("sync")
    local r3,r4=mountTo(DST)
    if r4~=0 then
      verdict("挂载失败","#ff7a7a")
      say(string.format("L%d M%d 已写入",r3,r4))
      say("重启手环后再试一次")
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
      say("先点下方「建副本」")
      return
    end
    if not restPath then verdict("缺少数据","#ff7a7a") return end
    say("正在还原…")
    local done,_,err=applyFpk(restPath,DST)
    if err then say(err) verdict("写入失败","#ff7a7a") return end
    sh("sync")
    local r3,r4=mountTo(DST)
    if r4~=0 then
      verdict("挂载失败","#ff7a7a")
      say(string.format("L%d M%d 已写入",r3,r4))
      say("重启手环后再试一次")
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
      verdict("源文件异常","#ff7a7a")
      say("app.bin 大小 "..s)
      return
    end
    if size_of(DST)==TOTAL then
      verdict("副本已就绪","#63D9A0")
      say("直接应用封面即可")
      return
    end
    say("复制 42MB 请勿退出…")
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
      verdict("本来就没有副本","#63D9A0")
      say("空间已干净")
      armed=false
      return
    end
    if not armed then
      armed=true
      verdict("确认删除？","#f0c060")
      say("再点一次「删副本」")
      return
    end
    armed=false
    say("正在删除…")
    -- 先解除占用：卸载挂载点并解绑 loop，否则空间不会真正回收
    sh("umount /resource/app")
    sh("umount -l /resource/app")
    sh("losetup -d /dev/loop0")
    pcall(function() os.remove(DST) end)
    sh("sync")
    if size_of(DST)<0 then
      verdict("已删除","#63D9A0")
      say("释放约 42MB；换装前需重建")
    else
      verdict("删除失败","#ff7a7a")
      say("可能被占用，重启后再试")
    end
  end)
  mkbtn(392,40,10,192,"体检状态","#B8B8B8",function()
    reset()
    local n,o,x=scanAll()
    say(string.format("新 %d · 旧 %d · 其他 %d",n,o,x))
    if size_of(DST)~=TOTAL then verdict("缺少副本","#ff7a7a")
    elseif n==#ENT then verdict("全部已换","#63D9A0")
    else verdict("待应用","#f0c060") end
  end,F_BIG)
  pcall(function()
    lvgl.Label(root,{x=14,y=448,w=184,h=54,text="应用后切表盘生效\n重启可完全恢复",text_font=F_BODY,text_color="#5A5A5A",text_align=2})
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
