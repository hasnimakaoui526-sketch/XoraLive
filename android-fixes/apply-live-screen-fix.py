from pathlib import Path
import re
p=Path("app/src/main/assets/home.html")
s=p.read_text(encoding="utf-8")
# Fix RealtimeKit API usage: enableVideo/enableAudio take no MediaStreamTrack argument.
s2=re.sub(r"\.enableVideo\(\s*videoTrack\s*\)", ".enableVideo()", s)
s2=re.sub(r"\.enableAudio\(\s*audioTrack\s*\)", ".enableAudio()", s2)
# Ensure the custom element is registered before it is mounted.
if "await customElements.whenDefined('rtk-meeting')" not in s2 and 'await customElements.whenDefined("rtk-meeting")' not in s2:
    s2=s2.replace("const el=document.createElement('rtk-meeting');", "await customElements.whenDefined('rtk-meeting');const el=document.createElement('rtk-meeting');", 1)
css=Path("android-fixes/live-screen-fix.css").read_text(encoding="utf-8")
if "xora-live-layout-final-2026-11" not in s2:
    s2 += "\n<style id='xora-live-layout-final-2026-11'>\n" + css + "\n</style>\n"
if s2==s:
    raise SystemExit("Patch did not match any source code; refusing to build an unchanged APK.")
p.write_text(s2,encoding="utf-8")
print("Applied Xora Live broadcast layout/API patch.")
