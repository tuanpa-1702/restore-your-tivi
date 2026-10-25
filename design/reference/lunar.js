// Vietnamese lunar calendar (Ho Ngoc Duc's algorithm, UTC+7). solarToLunar(25, 10, 2026) -> {day:16, month:9, year:2026, leap:false}
const LunarVN = (() => {
  const TZ = 7, PI = Math.PI, INT = Math.floor;
  const jd = (d, m, y) => {
    const a = INT((14 - m) / 12), yy = y + 4800 - a, mm = m + 12 * a - 3;
    let j = d + INT((153 * mm + 2) / 5) + 365 * yy + INT(yy / 4) - INT(yy / 100) + INT(yy / 400) - 32045;
    if (j < 2299161) j = d + INT((153 * mm + 2) / 5) + 365 * yy + INT(yy / 4) - 32083;
    return j;
  };
  const newMoon = k => {
    const T = k / 1236.85, T2 = T * T, T3 = T2 * T, dr = PI / 180;
    let J = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
    J += 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
    const M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
    const Mp = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
    const F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
    let C = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
    C += -0.4068 * Math.sin(Mp * dr) + 0.0161 * Math.sin(dr * 2 * Mp) - 0.0004 * Math.sin(dr * 3 * Mp);
    C += 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mp)) - 0.0074 * Math.sin(dr * (M - Mp));
    C += 0.0004 * Math.sin(dr * (2 * F + M)) - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mp));
    C += 0.0010 * Math.sin(dr * (2 * F - Mp)) + 0.0005 * Math.sin(dr * (2 * Mp + M));
    const dt = T < -11 ? 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
                       : -0.000278 + 0.000265 * T + 0.000262 * T2;
    return INT(J + C - dt + 0.5 + TZ / 24);
  };
  const sunLong = j => {
    const T = (j - 2451545.5 - TZ / 24) / 36525, T2 = T * T, dr = PI / 180;
    const M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
    const L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
    let DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
    DL += (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.000290 * Math.sin(dr * 3 * M);
    let L = (L0 + DL) * dr;
    L -= PI * 2 * INT(L / (PI * 2));
    return INT(L / PI * 6);
  };
  const month11 = y => {
    const k = INT((jd(31, 12, y) - 2415021) / 29.530588853);
    let nm = newMoon(k);
    if (sunLong(nm) >= 9) nm = newMoon(k - 1);
    return nm;
  };
  const leapOffset = a11 => {
    const k = INT((a11 - 2415021.076998695) / 29.530588853 + 0.5);
    let last, i = 1, arc = sunLong(newMoon(k + i));
    do { last = arc; i++; arc = sunLong(newMoon(k + i)); } while (arc !== last && i < 14);
    return i - 1;
  };
  function solarToLunar(d, m, y) {
    const day = jd(d, m, y), k = INT((day - 2415021.076998695) / 29.530588853);
    let start = newMoon(k + 1);
    if (start > day) start = newMoon(k);
    let a11 = month11(y), b11 = a11, ly;
    if (a11 >= start) { ly = y; a11 = month11(y - 1); } else { ly = y + 1; b11 = month11(y + 1); }
    const ld = day - start + 1, diff = INT((start - a11) / 29);
    let leap = false, lm = diff + 11;
    if (b11 - a11 > 365) {
      const off = leapOffset(a11);
      if (diff >= off) { lm = diff + 10; if (diff === off) leap = true; }
    }
    if (lm > 12) lm -= 12;
    if (lm >= 11 && diff < 4) ly -= 1;
    return { day: ld, month: lm, year: ly, leap };
  }
  const CAN = ['Giáp', 'Ất', 'Bính', 'Đinh', 'Mậu', 'Kỷ', 'Canh', 'Tân', 'Nhâm', 'Quý'];
  const CHI = ['Tý', 'Sửu', 'Dần', 'Mão', 'Thìn', 'Tỵ', 'Ngọ', 'Mùi', 'Thân', 'Dậu', 'Tuất', 'Hợi'];
  const yearName = y => CAN[(y + 6) % 10] + ' ' + CHI[(y + 8) % 12];
  return { solarToLunar, yearName };
})();
