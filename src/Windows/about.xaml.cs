using System.Diagnostics;
using System.Windows;

namespace UsbExtractor
{
    /// <summary>
    /// Interaction logic for about.xaml
    /// </summary>
    public partial class about : Window
    {
        public about()
        {
            InitializeComponent();
            // Setting direct events for button github and website...
            _github.Click += (o, e) =>
            {
                Process.Start("https://github.com/KaustubhPatange/Iso2Usb");
            };
            _website.Click += (o, e) =>
            {
                Process.Start("");
            };
        }
    }
}
