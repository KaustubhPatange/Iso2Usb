# Iso2Usb: Create Bootable Media from ISO
![GitHub issues](https://img.shields.io/github/issues/kaustubhpatange/iso2usb.svg)
![GitHub language count](https://img.shields.io/github/languages/count/kaustubhpatange/iso2usb.svg)
[![Licence](https://img.shields.io/badge/license-GPLv3-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0.en.html)
![GitHub repo size](https://img.shields.io/github/repo-size/kaustubhpatange/iso2usb.svg)

![Logo](https://github.com/KaustubhPatange/Iso2Usb/raw/master/images/icon.png)

## Features
* Create UFEI bootable devices from ISO images
* Format USB with FAT32/vFAT, NTFS, EXT4
* Create virtual hard disk (vhd) from USB (windows only)
* Create dd out from USB block (linux only)
* Automatic detection & selection of cluster size, partiton type
* Create extended labels and icon files 
* Calculate md5 & sha1 hash keys
* Check updates automatically

## Compilation
### Windows
* Use either [Visual Studio 2017](https://visualstudio.microsoft.com/) or later to open file from src/Windows/*.sln file
### Linux
* Setup [Java JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) which includes JavaFx source and [Intellij IDEA](https://snapcraft.io/intellij-idea-community)
* In Intellij, select File > Open project and select folder from src/Linux
If you get any compiling error, make sure to set your default java jdk as jdk8. Also let me know you, the project will not compile over jdk11+ environment since oracle decided to remove JavaFX from package.

## License

* [General Publication License 3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)

```
Copyright 2019 Kaustubh Patange

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```