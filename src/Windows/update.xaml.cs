using Adb_gui_Apkbox_plugin;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace UsbExtractor
{
    /// <summary>
    /// Interaction logic for update.xaml
    /// </summary>
    public partial class update : Window
    {
        string settingfile = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData) + "\\iso2usb.ini";
        public update(int index)
        {
            InitializeComponent();
            _close.Click += (o, e) => { Close(); };
            _updates.SelectedIndex = index;
            _updates.SelectionChanged += _updates_SelectionChanged;
        }
        /// <summary>
        /// Event will be executed when value in combo box will be changed...
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void _updates_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (_updates.SelectedIndex == 0)
                File.WriteAllLines(settingfile, new IniFile(File.ReadAllText(settingfile)).Write("checkupdates", "no"));
            else
                File.WriteAllLines(settingfile, new IniFile(File.ReadAllText(settingfile)).Write("checkupdates", "yes"));
        }
    }
}
