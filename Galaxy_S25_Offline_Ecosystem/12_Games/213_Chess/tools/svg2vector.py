#!/usr/bin/env python3
"""cburnett taş SVG'lerini Android VectorDrawable XML'ine çevirir (yalnızca bu setteki öğeler: g, path, circle).
Kullanım: svg2vector.py <svg_klasörü> <res/drawable klasörü>"""
import os, sys, xml.etree.ElementTree as ET
NS = '{http://www.w3.org/2000/svg}'
INHERIT = ['fill', 'stroke', 'stroke-width', 'stroke-linecap', 'stroke-linejoin', 'fill-rule']
DEF = {'fill': '#000', 'stroke': 'none', 'stroke-width': '1', 'stroke-linecap': 'butt', 'stroke-linejoin': 'miter', 'fill-rule': 'nonzero'}

def color(c):
    if c in (None, 'none'): return None
    c = c.strip()
    if c.startswith('#') and len(c) == 4: c = '#' + ''.join(ch * 2 for ch in c[1:])
    return '#FF' + c[1:].upper()

def circle_path(cx, cy, r):
    return f'M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0z'

def walk(el, style, out):
    st = dict(style)
    for k in INHERIT:
        if el.get(k) is not None: st[k] = el.get(k)
    tag = el.tag.replace(NS, '')
    if tag == 'path': out.append((el.get('d'), st))
    elif tag == 'circle': out.append((circle_path(float(el.get('cx')), float(el.get('cy')), float(el.get('r'))), st))
    elif tag in ('g', 'svg'):
        for ch in el: walk(ch, st, out)
    else: raise SystemExit(f'desteklenmeyen öğe: {tag}')

def convert(src, dst):
    paths = []; walk(ET.parse(src).getroot(), DEF, paths)
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<!-- Colin M.L. Burnett "cburnett" taş seti, GPLv2+ (lichess-org/lila public/piece/cburnett). tools/svg2vector.py ile üretildi. -->',
             '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="45dp" android:height="45dp" android:viewportWidth="45" android:viewportHeight="45">']
    for d, st in paths:
        a = [f'android:pathData="{d}"']
        f = color(st['fill']); s = color(st['stroke'])
        if f: a.append(f'android:fillColor="{f}"')
        if st['fill-rule'] == 'evenodd': a.append('android:fillType="evenOdd"')
        if s:
            a += [f'android:strokeColor="{s}"', f'android:strokeWidth="{st["stroke-width"]}"',
                  f'android:strokeLineCap="{st["stroke-linecap"]}"', f'android:strokeLineJoin="{st["stroke-linejoin"]}"']
        lines.append('    <path ' + ' '.join(a) + ' />')
    lines.append('</vector>')
    open(dst, 'w').write('\n'.join(lines) + '\n')

src_dir, dst_dir = sys.argv[1], sys.argv[2]
for f in sorted(os.listdir(src_dir)):
    if f.endswith('.svg'):
        name = 'piece_' + f[:-4].lower()  # wK -> piece_wk
        convert(os.path.join(src_dir, f), os.path.join(dst_dir, name + '.xml'))
        print(name)
