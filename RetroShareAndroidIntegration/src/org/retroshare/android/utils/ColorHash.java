/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.retroshare.android.utils;

public class ColorHash
{
	public static final int getObjectColor(Object o) { return getNumberColor(o.hashCode()); }
	public static final int getNumberColor(int o) { return (0xFF000000 | ColorArray[Math.abs(o % ColorArray.length)]); }

	// Colors taken from http://developer.android.com/design/style/color.html
	public static final int[] ColorArray = new int[]
	{
		0xE2F4FB, 0xC5EAF8, 0xA8DFF4, 0x8AD5F0, 0x6DCAEC, 0x50C0E9, 0x33B5E5, 0x2CB1E1, 0x24ADDE, 0x1DA9DA, 0x16A5D7, 0x0FA1D3, 0x079DD0, 0x0099CC, // Blue Gradations
		0xF5EAFA, 0xE5CAF2, 0xDDBCEE, 0xD6ADEB, 0xCF9FE7, 0xCB97E5, 0xC58BE2, 0xC182E0, 0xBA75DC, 0xB368D9, 0xAC59D6, 0xA750D3, 0xA041D0, 0xF5F5F5, // Violet Gradations
		0xF0F8DB, 0xE2F0B6, 0xD3E992, 0xC5E26D, 0xB6DB49, 0xA8D324, 0x99CC00, 0x92C500, 0x8ABD00, 0x83B600, 0x7CAF00, 0x75A800, 0x6DA000, 0x669900, // Green Gradations
		0xFFF6DF, 0xFFECC0, 0xFFE3A0, 0xFFD980, 0xFFD060, 0xFFC641, 0xFFBD21, 0xFFB61C, 0xFFAE18, 0xFFA713, 0xFFA00E, 0xFF9909, 0xFF9105, 0xFD8903, // Yellow Gradations
		0xFFE4E4, 0xFFCACA, 0xFFAFAF, 0xFF9494, 0xFF7979, 0xFF5F5F, 0xFF4444, 0xF83A3A, 0xF03131, 0xE92727, 0xE21D1D, 0xDB1313, 0xD30A0A, 0xCC0000  // Red Gradations
	};
}
