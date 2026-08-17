"""
Prueft, ob aufgerufene Minecraft-Methoden ueberhaupt zugaenglich sind.

Der Fehler, den das faengt: isCurrentlyBreaking ist private. Die Yarn-Mappings
verzeichnen keine Sichtbarkeiten -- dort steht die Methode wie jede andere, und
alle bisherigen Pruefungen sahen nichts. Erst der Compiler meldete es.

Gelesen wird deshalb direkt aus den Klassendateien des Spiels, wo die
Sichtbarkeit tatsaechlich steht.

Braucht mc-merged.jar (entsteht beim Bauen, liegt unter vortex-work).
"""
import struct, zipfile, re, glob, sys, os

JAR = '/home/claude/mcref/mc-merged.jar'
PUBLIC, PRIVATE, PROTECTED = 0x0001, 0x0002, 0x0004


def parse_class(data):
    """(Name -> Zugriffsflags) aller Methoden einer Klassendatei."""
    if data[:4] != b'\xca\xfe\xba\xbe':
        return {}
    i = 10
    n = struct.unpack('>H', data[8:10])[0]
    pool, idx = {}, 1
    while idx < n:
        tag = data[i]; i += 1
        if tag == 1:
            ln = struct.unpack('>H', data[i:i + 2])[0]; i += 2
            pool[idx] = data[i:i + ln].decode('utf-8', 'replace'); i += ln
        elif tag in (7, 8, 16, 19, 20):
            i += 2
        elif tag == 15:
            i += 3
        elif tag in (5, 6):
            i += 8; idx += 1
        else:
            i += 4
        idx += 1
    i += 6
    ifc = struct.unpack('>H', data[i:i + 2])[0]; i += 2 + ifc * 2
    result = {}
    for runde in range(2):
        cnt = struct.unpack('>H', data[i:i + 2])[0]; i += 2
        current = {}
        for _ in range(cnt):
            acc, nm, ds = struct.unpack('>HHH', data[i:i + 6]); i += 6
            at = struct.unpack('>H', data[i:i + 2])[0]; i += 2
            for _ in range(at):
                al = struct.unpack('>I', data[i + 2:i + 6])[0]; i += 6 + al
            name = pool.get(nm)
            if name:
                current.setdefault(name, 0)
                current[name] |= acc
        result = current
    return result


def main():
    if not os.path.exists(JAR):
        print("  (mc-merged.jar fehlt -- Pruefung uebersprungen)")
        return 0

    # Namen sammeln, die IRGENDWO oeffentlich sind, und solche, die es
    # nirgends sind.
    #
    # Nur die zweite Gruppe wird gemeldet. Ohne echte Typanalyse laesst sich
    # nicht sagen, welche Klasse gemeint ist -- und Namen wie getX gibt es in
    # hunderten Klassen, meist oeffentlich. Wuerde jeder Treffer gemeldet,
    # kaemen 229 Fehlalarme heraus und niemand laese die Ausgabe je wieder.
    #
    # Ist ein Name dagegen in KEINER Klasse oeffentlich, ist der Aufruf sicher
    # falsch, egal auf welchem Typ.
    irgendwo_oeffentlich = set()
    nicht_oeffentlich = {}
    with zipfile.ZipFile(JAR) as z:
        for name in z.namelist():
            if not name.endswith('.class') or '$' in name:
                continue
            if not name.startswith('net/minecraft/'):
                continue
            try:
                ms = parse_class(z.read(name))
            except Exception:
                continue
            cls = name[:-6].split('/')[-1]
            for m, acc in ms.items():
                # Nur PRIVATE zaehlt als unerreichbar. Protected und paketweit
                # sind aus einer Unterklasse heraus voellig zulaessig -- eine
                # Mixin-Klasse erbt von Screen und darf addDrawableChild rufen.
                # Ueberladungen teilen sich den Namen: drawItem gibt es in
                # DrawContext mehrfach, oeffentlich und privat. Die Flags werden
                # oben zusammengefasst, deshalb zaehlt hier nur, was WEDER
                # oeffentlich ist -- sonst gilt eine Methode als unerreichbar,
                # obwohl eine ihrer Formen laengst benutzt wird.
                if (acc & PRIVATE) and not (acc & PUBLIC):
                    nicht_oeffentlich.setdefault(m, set()).add(cls)
                else:
                    irgendwo_oeffentlich.add(m)

    # Alles, was irgendwo oeffentlich ist, bleibt unbeanstandet.
    for m in irgendwo_oeffentlich:
        nicht_oeffentlich.pop(m, None)

    # Methoden des Java-Standards. Sie heissen zufaellig genauso wie eine
    # private Methode irgendwo in Minecraft -- substring gibt es in String und
    # in DataCommand, currentTimeMillis in System und in einer Tracker-Klasse.
    # Ohne diese Liste kamen 111 Fehlalarme allein aus dem Client.
    JAVA_STANDARD = {
        'substring','currentTimeMillis','nanoTime','length','charAt','indexOf',
        'equals','hashCode','toString','compareTo','trim','split','replace',
        'startsWith','endsWith','contains','isEmpty','format','valueOf','append',
        'add','remove','get','put','size','clear','contains','iterator','stream',
        'min','max','abs','round','sqrt','floor','ceil','random','printStackTrace',
        'getMessage','name','ordinal','values','join','toLowerCase','toUpperCase',
        'parseInt','parseDouble','sleep','start','run','close','flush','write',
        'read','set','isPresent','orElse','getKey','getValue','forEach','sort',
        'compare','compareTo','merge','apply','accept','test','of','copyOf',
    }

    # Eigene Methodennamen sammeln. drawBox etwa gehoert uns selbst und hat
    # mit der gleichnamigen privaten Methode in einem Widget nichts zu tun.
    eigene = set()
    for path in glob.glob('src/**/*.java', recursive=True):
        eigene |= set(re.findall(
            r'(?:public|private|protected|static|final|\s)+[\w.<>\[\]]+\s+(\w+)\s*\([^;]*\)\s*\{',
            open(path).read()))

    problems = []
    for path in sorted(glob.glob('src/**/*.java', recursive=True)):
        src = open(path).read()
        src = re.sub(r'//[^\n]*', '', re.sub(r'/\*[\s\S]*?\*/', '', src))
        # Aufrufe der Form etwas.methode(
        for m in re.finditer(r'\.(\w+)\s*\(', src):
            name = m.group(1)
            if name in JAVA_STANDARD or name in eigene:
                continue
            if name in nicht_oeffentlich:
                zeile = src[:m.start()].count('\n') + 1
                besitzer = sorted(nicht_oeffentlich[name])[:3]
                problems.append((path.split('/')[-1], zeile, name, besitzer))

    for fn, ln, name, owner in problems:
        print(f"  {fn}:{ln}  '{name}' ist in {', '.join(owner)} nicht oeffentlich")
    print(f"  {'✓ keine nicht-oeffentlichen Aufrufe' if not problems else str(len(problems)) + ' Verdachtsfaelle'}")
    return 1 if problems else 0


sys.exit(main())
