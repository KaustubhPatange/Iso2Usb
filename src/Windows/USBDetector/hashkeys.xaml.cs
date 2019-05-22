using System;
using System.Collections.Generic;
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

namespace UsbExtractor.USBDetector
{
    /// <summary>
    /// Interaction logic for hashkeys.xaml
    /// </summary>
    public partial class hashkeys : Window
    {
        public hashkeys(string md5,string sha1)
        {
            InitializeComponent();
            _md5.Text = md5;
            _sha1.Text = sha1;
        }

        private void _close_Click(object sender, RoutedEventArgs e)
        {
            this.Close();
        }
    }
}
