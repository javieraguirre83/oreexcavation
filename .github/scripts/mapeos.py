"""Builds a SRG -> official-name table for Minecraft 1.20.1 (to read the original 1.20.1 mod)."""
import io, json, re, sys, urllib.request, zipfile

OUT = sys.argv[1]
VER = "1.20.1"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (OreExcavation port build)"})
    with urllib.request.urlopen(req) as r:
        return r.read()

# Mojang official mappings (official -> obf)
manifest = json.loads(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
vurl = next(v["url"] for v in manifest["versions"] if v["id"] == VER)
vjson = json.loads(get(vurl))
moj = get(vjson["downloads"]["client_mappings"]["url"]).decode()

# MCP config (obf -> srg)
try:
    meta = get("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/maven-metadata.xml").decode()
    mcp_ver = [v for v in re.findall(r"<version>([^<]+)</version>", meta) if v.startswith(VER + "-")][-1]
except Exception as e:
    print("metadata failed", e)
    mcp_ver = "1.20.1-20230612.114412"
z = zipfile.ZipFile(io.BytesIO(get(f"https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/{mcp_ver}/mcp_config-{mcp_ver}.zip")))
tsrg = z.read("config/joined.tsrg").decode()

# --- parse mojang ---
cls_off2obf = {}
members = {}  # obf_class -> list of (kind, official_name, official_desc_or_type, obfname)
cur = None
for line in moj.splitlines():
    if line.startswith("#") or not line.strip():
        continue
    if not line.startswith(" "):
        off, obf = line.rstrip(":").split(" -> ")
        cls_off2obf[off] = obf
        cur = obf
        members[cur] = []
    else:
        left, obfname = line.strip().split(" -> ")
        left = re.sub(r"^\d+:\d+:", "", left)
        if "(" in left:
            ret, rest = left.split(" ", 1)
            name, args = rest.split("(", 1)
            args = args.rstrip(")")
            members[cur].append(("m", name, (ret, args), obfname))
        else:
            typ, name = left.split(" ", 1)
            members[cur].append(("f", name, typ, obfname))

PRIM = {"int": "I", "long": "J", "float": "F", "double": "D", "boolean": "Z", "byte": "B", "char": "C", "short": "S", "void": "V"}

def desc(t):
    dims = t.count("[]")
    base = t.replace("[]", "")
    if base in PRIM:
        d = PRIM[base]
    else:
        d = "L" + cls_off2obf.get(base, base).replace(".", "/") + ";"
    return "[" * dims + d

# --- parse tsrg (obf -> srg) ---
srg = {}  # (obfclass, obfname, obfdesc or None) -> srgname
cur = None
for line in tsrg.splitlines():
    if line.startswith("tsrg2") or not line.strip():
        continue
    if not line.startswith("\t"):
        cur = line.split()[0]
        continue
    if line.startswith("\t\t"):
        continue
    parts = line.strip().split()
    if len(parts) >= 3 and parts[1].startswith("("):
        srg[(cur, parts[0], parts[1])] = parts[2]
    elif len(parts) >= 2:
        srg[(cur, parts[0], None)] = parts[1]

out = {}
for obfcls, mem in members.items():
    for kind, name, info, obfname in mem:
        if kind == "f":
            s = srg.get((obfcls, obfname, None))
        else:
            ret, args = info
            d = "(" + "".join(desc(a) for a in args.split(",") if a) + ")" + desc(ret)
            s = srg.get((obfcls, obfname, d))
        if s and (s.startswith("m_") or s.startswith("f_")):
            off_cls = next((k for k, v in cls_off2obf.items() if v == obfcls), obfcls) if False else None
            out.setdefault(s, set()).add(name)

with open(OUT, "w") as f:
    for s in sorted(out):
        f.write(s + " " + "|".join(sorted(out[s])) + "\n")
print("mappings:", len(out), "mcp", mcp_ver)
