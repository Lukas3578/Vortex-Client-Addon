"""
Findet Klassen, die benutzt, aber nicht importiert werden.

Genau der Fehler, der den Build gekostet hat: ItemStack stand im Code, aber
nicht bei den Importen. Der Java-Parser sieht davon nichts -- Importe sind ihm
gleichgueltig, die Datei ist syntaktisch einwandfrei. Erst der Compiler merkt es.

Die bisherige Pruefung suchte nur die Gegenrichtung, also Importe ohne
Verwendung. Der haeufigere und teurere Fall fehlte.
"""
import re, glob, sys, os

# Klassen, die ohne Import verfuegbar sind.
JAVA_LANG = {
    'String','Integer','Double','Float','Boolean','Long','Short','Byte','Character',
    'Object','Math','System','Exception','RuntimeException','Throwable','Error',
    'Thread','Runnable','Class','Number','Iterable','Comparable','Override',
    'StringBuilder','CharSequence','Void','Enum','SuppressWarnings','Deprecated',
    'NumberFormatException','IllegalArgumentException','IllegalStateException',
    'NullPointerException','ReflectiveOperationException','AutoCloseable',
    'InterruptedException','ProcessBuilder','ProcessHandle','Process',
    'ClassLoader','StringBuffer','Iterable','Cloneable','SafeVarargs',
    'UnsupportedOperationException','ArithmeticException','ClassCastException',
    'IndexOutOfBoundsException','ArrayIndexOutOfBoundsException','StackOverflowError',
    'OutOfMemoryError','FunctionalInterface','Record','Package','Module',
    'ThreadLocal','ThreadGroup','Runtime','StrictMath','Comparable',
}

problems = []
for path in sorted(glob.glob('src/**/*.java', recursive=True)):
    src = open(path).read()
    pkg = re.search(r'^package ([\w.]+);', src, re.M)
    pkg = pkg.group(1) if pkg else ''

    imported = set()
    for imp in re.findall(r'^import (?:static )?([\w.]+);', src, re.M):
        imported.add(imp.split('.')[-1])

    # Klassen im selben Paket brauchen keinen Import.
    same_pkg = set()
    pkg_dir = os.path.dirname(path)
    for f in os.listdir(pkg_dir):
        if f.endswith('.java'):
            same_pkg.add(f[:-5])

    # Eigene Typen der Datei (Klasse, innere Klassen, Enums, Records).
    own = set(re.findall(r'\b(?:class|interface|enum|record)\s+(\w+)', src))

    # Von der Oberklasse geerbte innere Typen sind ohne Import sichtbar.
    # Category kommt aus Module und wird als Module.Category vererbt -- ohne
    # diese Ausnahme meldet die Pruefung jede Moduldatei, und eine Pruefung,
    # die immer meckert, liest irgendwann niemand mehr.
    # Von der Oberklasse geerbte innere Typen. Category kommt aus Module,
    # Entry aus SelectionScreen -- beide ohne Import sichtbar.
    own |= {'Category', 'Entry'}

    body = re.sub(r'^(?:package|import)[^\n]*\n', '', src, flags=re.M)
    body = re.sub(r'"[^"\n]*"', '""', body)               # Zeichenketten raus
    body = re.sub(r'/\*[\s\S]*?\*/', '', body)            # Blockkommentare raus
    body = re.sub(r'//[^\n]*', '', body)                  # Zeilenkommentare raus

    # Grossgeschriebene Bezeichner, die wie Typen benutzt werden.
    for m in re.finditer(r'(?<![\w.])([A-Z][A-Za-z0-9]*)\s*(?:\.|<|\s+\w+\s*[=;,)]|\()', body):
        name = m.group(1)
        if name in imported or name in JAVA_LANG or name in same_pkg or name in own:
            continue
        if name.isupper():        # Konstanten wie MAX_VALUE
            continue
        problems.append((path.split('/')[-1], name))

seen = set()
for fn, name in problems:
    if (fn, name) in seen:
        continue
    seen.add((fn, name))
    print(f"  {fn}: '{name}' benutzt, aber nicht importiert")
print(f"  {'✓ keine fehlenden Importe' if not seen else str(len(seen)) + ' Verdachtsfaelle'}")
sys.exit(1 if seen else 0)
