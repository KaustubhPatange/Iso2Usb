using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Linq;

// An extract of code I made for my other project ADK https://androdevkit.github.io
namespace Adb_gui_Apkbox_plugin
{
    class IniFile 
    {
        string[] lines;
        public IniFile(string text)
        {
            lines = Regex.Split(text, "\r\n");
        }
        public string Read(string key)
        {
            foreach (var line in from string line in lines where line.StartsWith(key) select line)
            {
                try
                {
                    return line.Split('=')[1].Trim();
                }
                catch { return ""; };
            }

            return "";
        }
        public string[] Write(string key, string value)
        {
            for(int i=0;i<lines.Length;i++)
            {
                if (lines[i].StartsWith(key))
                {
                    lines[i] = $"{key} = {value}";
                }
            }
            return lines;
        }
    }
}