#!/usr/bin/env bash
set -euo pipefail
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/dev/local/ringermodetile"
cp app/src/main/java/dev/local/ringermodetile/Modes.java "$tmp/dev/local/ringermodetile/"
cat > "$tmp/dev/local/ringermodetile/R.java" <<'EOF'
package dev.local.ringermodetile;
final class R {
    static class drawable { static int ic_silent=1, ic_vibrate=2, ic_ring=3; }
    static class string { static int silent=4, vibrate=5, normal=6; }
}
EOF
cat > "$tmp/dev/local/ringermodetile/Test.java" <<'EOF'
package dev.local.ringermodetile;
public class Test {
    static void expect(int got, int want) { if (got != want) throw new AssertionError(got+" != "+want); }
    public static void main(String[] args) {
        expect(Modes.next(2,true),1);
        expect(Modes.next(1,true),0);
        expect(Modes.next(0,true),2);
        expect(Modes.next(2,false),0);
        expect(Modes.next(0,false),2);
        expect(Modes.next(-1,true),2);
        expect(Modes.icon(0),R.drawable.ic_silent);
        expect(Modes.icon(1),R.drawable.ic_vibrate);
        expect(Modes.icon(2),R.drawable.ic_ring);
        expect(Modes.label(0),R.string.silent);
        expect(Modes.label(1),R.string.vibrate);
        expect(Modes.label(2),R.string.normal);
        System.out.println("12 assertions passed");
    }
}
EOF
javac -d "$tmp/out" "$tmp"/dev/local/ringermodetile/*.java
java -cp "$tmp/out" dev.local.ringermodetile.Test
