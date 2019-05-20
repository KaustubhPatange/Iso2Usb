using Dolinay;
using kp.Iso2Linux;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;

namespace UsbExtractor
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        // Using drive detector class from https://www.codeproject.com/articles/18062/detecting-usb-drive-removal-in-a-c-program
        private DriveDetector driveDetector; long volumeSize = 0; string volumeLabel = null;
        private string temp; DispatcherTimer timer,timer2; BackgroundWorker main;
        public MainWindow()
        {
            InitializeComponent();
            Disable();
            Title = "Iso2Usb - " + Assembly.GetExecutingAssembly().GetName().Version.ToString() + " (Portable)";
        }
        /// <summary>
        /// This event will occur when the window is closing i.e application is about to close.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void Window_Closing(object sender, CancelEventArgs e)
        {
            DeleteDirectory(temp, true);
        }
        /// <summary>
        /// Overriding existing onContentRendered view, this will occur when form is shown. I did this because running this events
        /// under constructor is making window minimized.
        /// </summary>
        /// <param name="e"></param>
        protected override void OnContentRendered(EventArgs e)
        {
            base.OnContentRendered(e);
            // Allocating new memory for Drive Detector class and settting events...
            driveDetector = new DriveDetector();
            driveDetector.DeviceArrived += new DriveDetectorEventHandler(OnDriveArrived);
            driveDetector.DeviceRemoved += new DriveDetectorEventHandler(OnDriveRemoved);
            // Some other codes like extracting tools used by Iso2Usb
            _startcancelButton.IsEnabled = false;
            temp = GetTemporaryDir();
            File.WriteAllBytes(temp + "\\7z.exe", Properties.Resources._7z1);
            File.WriteAllBytes(temp + "\\7z.dll", Properties.Resources._7z);
            // Setting partition Type combo box...
            _partitionCombo.Items.Add("MBR");
            _partitionCombo.Items.Add("GPT");
            _partitionCombo.SelectedIndex = 0;
            // Setting clustor size combo box... (What is Clustor size ? -- Best Answer so far https://support.microsoft.com/en-us/help/140365/default-cluster-size-for-ntfs-fat-and-exfat)
            DetectClusterSize();
            // Setting file system type combo box...
            _filesystemCombo.Items.Add("FAT32/vfat (Default)");
            _filesystemCombo.Items.Add("NTFS");
            _filesystemCombo.SelectedIndex = 0;
            // Method to detect already connected USBs...
            DetectUSB();
        }
        /// <summary>
        /// This event occur when any new usb is inserted.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void OnDriveArrived(object sender, DriveDetectorEventArgs e)
        {
            var drives = DriveInfo.GetDrives();
            foreach (DriveInfo drive in drives)
            {
                if (drive.Name == e.Drive)
                {
                    // Add the drive...
                    TextBlock label = new TextBlock();
                    label.Text = drive.VolumeLabel + " (" + drive.Name.Replace("\\", "") + ") [" + SizeSuffix(drive.TotalSize) + "]";
                    label.Tag = drive.Name.Remove(drive.Name.Length - 2);
                    _usbdriveCombo.Items.Add(label);
                    Log("USB Inserted in volume " + drive.Name.Replace("\\", ""));
                    loadDefaultDrive();
                }
            }
        }

        /// <summary>
        /// This event will be executed when any connected usb drive is removed.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void OnDriveRemoved(object sender, DriveDetectorEventArgs e)
        {
            string driveletter = e.Drive.FirstOrDefault().ToString();
            for (int i = 0; i < _usbdriveCombo.Items.Count; i++)
            {
                TextBlock label = _usbdriveCombo.Items[i] as TextBlock;
                string text = label.Text;
                if (text.Contains(driveletter + ":)"))
                {
                    // Remove the drive from box...
                    _usbdriveCombo.Items.RemoveAt(i);
                    Log("Volume removed " + e.Drive.Replace("\\", ""));
                }
                loadDefaultDrive();
            }
        }
        /// <summary>
        /// A click event for _browseButton
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void _browseButton_Click(object sender, RoutedEventArgs e)
        {
            OpenFileDialog ofd = new OpenFileDialog();
            ofd.Title = "Select a file";
            ofd.Filter = "Supported Formats|*.iso";
            ofd.FileName = "";
            if (ofd.ShowDialog() == true)
            {
                TextBlock label = new TextBlock();
                label.Tag = ofd.FileName;
                label.Text = Path.GetFileName(ofd.FileName);
                _fileCombo.IsEnabled = true;
                _fileCombo.Items.Add(label);
                try
                {
                    _fileCombo.SelectedIndex = _fileCombo.Items.Count - 1;
                }
                catch (Exception ex) { Log("Error: " + ex.Message, true); }
                loadFile(ofd.FileName);
            }
        }
        /// <summary>
        /// This event will occur when start or stop button is clicked
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void _startcancelButton_Click(object sender, RoutedEventArgs e)
        {
            if ((string)_startcancelButton.Content == "START")
            {
                // Start the process from here...
                var msg = MessageBox.Show("All data stored in USB drive will be erased during this process. Make sure to take backup if neccessary.\n\nAre you sure to continue?",
                    "Notice", MessageBoxButton.YesNo, MessageBoxImage.Warning);
                if (msg == MessageBoxResult.Yes)
                {
                    // Step 0: Getting drive letter and setting up...
                    TextBlock label = _usbdriveCombo.SelectedItem as TextBlock;
                    TextBlock file = _fileCombo.SelectedItem as TextBlock;
                    string driveletter = label.Tag as string; string volumelabel = _labelTextBox.Text;
                    string filename = file.Tag as string;
                    string clustersize = "4096"; bool quickformat = false;
                    DriveExtender.FileSystemType ftype = DriveExtender.FileSystemType.FAT32;
                    DriveExtender.PartitionType ptype = DriveExtender.PartitionType.MBR;
                    if (_filesystemCombo.SelectedItem as string == "NTFS") ftype = DriveExtender.FileSystemType.NTFS;
                    if (_partitionCombo.SelectedItem as string == "GPT") ptype = DriveExtender.PartitionType.GPT;
                    if (_quickformatCheckBox.IsChecked==true) quickformat = true;
                    clustersize = (_clusterCombo.SelectedItem as string).Split(' ')[0];
                    _startcancelButton.Content = "ABORT"; Disable();
                    // Starting timer...
                    int _min = 0, _sec = 0;
                    timer2 = new DispatcherTimer(new TimeSpan(0, 0, 1), DispatcherPriority.Normal, delegate
                    {
                        _sec++;
                        if (_sec>=60) { _min++; _sec = 0; }
                        _progresslabel.Text = $"Running: {_min} min {_sec} sec";
                    }, this.Dispatcher);
                    timer2.Start();

                    /* 
                     * While labeling a volume, there are some limitations. If you are labeling a FAT volume, you can use 11 characters, 
                     * while NTFS volumes can use up to 32 characters. Your labels cannot include tabs but you can use spaces. 
                     * If you are labeling an NTFS drive, you can use all characters, however, FAT volumes cannot be labeled with the 
                     * following characters { * ? / \ | . , ; : + = [ ] < > " }
                     */

                    if (ftype==DriveExtender.FileSystemType.FAT32)
                    {
                        string[] exclude = new string[] { "*", "?", "/", "\\", "|", ".", ",", ";", ":", "+", "=", "[", "]", "<", ">", "\"" };
                        foreach(string str in exclude)
                        {
                            if (volumelabel.Contains(str))
                                volumelabel = volumelabel.Replace(str, "_");
                        }
                        volumelabel = new string(volumelabel.Take(11).ToArray());
                    }else if (ftype == DriveExtender.FileSystemType.NTFS)
                    {
                        volumelabel = new string(volumelabel.Take(32).ToArray());
                    }

                    // Using my DriveExtender class to create a reference to this drive...
                    DriveExtender driveExt = new DriveExtender(driveletter);
                    // Step 1: Formating the USB drive...
                    Log($"Formating '{label.Text}'");
                    _progressBar.IsIndeterminate = true;       
                    Task factory = Task.Factory.StartNew(() =>
                    {
                        driveExt.Format(ftype, ptype, volumelabel.Trim(), clustersize, quickformat);
                    });
                    do { DoEvents(); } while (!factory.IsCompleted);
                    _progressBar.IsIndeterminate = false;
                    // Step 2: Extracting ISO...
                    Log("Extracting ISO to " + driveletter + ":\\");
                    ExtractISO(filename, driveletter + ":\\");
                    _startcancelButton.Content = "START";
                    DeleteDirectory(driveletter + ":\\[BOOT]", true);
                    Log("Done");
                    _progressBar.Value = 0;
                    timer2.Stop();
                    Log($"Ran for {_min} min {_sec} sec");
                    MessageBox.Show("Bootable media has been created!", "Notice", MessageBoxButton.OK, MessageBoxImage.Information);
                }
                else return;
            }
            else
            {
                // Stop the process of extraction from here...
                Disable();
                _progressBar.IsIndeterminate = true;
                Log($"Cancelling... Please Wait!");
                timer.Stop(); timer2.Stop(); main.CancelAsync();
                _progressBar.Foreground = new SolidColorBrush(Colors.Yellow);
                Task run = Task.Factory.StartNew(() =>
                {
                    Thread.Sleep(2000);
                    
                      KillProcess("diskpart.exe");
                      KillProcess("7z.exe");
                });
                do { DoEvents(); } while (!run.IsCompleted);
                _progressBar.IsIndeterminate = false;
                Log($"Done");
                Enable();
                _progressBar.Value = 0;
                return;
            }
            _progressBar.IsIndeterminate = false;
            DetectUSB();
            Enable();
            _progressBar.Value = 0;
        }
        /// <summary>
        /// This event will occur when value in file selection combo box is changed
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void _fileCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            // Loading file which is stored in label's tag...
            TextBlock label = _fileCombo.SelectedItem as TextBlock;
            loadFile((string)label.Tag);
        }
        /// <summary>
        /// This event will occur when value in usb drive combo is changed
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void _usbdriveCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            loadDefaultDrive();
        }
        /// <summary>
        /// This will use to analyse iso file...
        /// </summary>
        /// <param name="fileName"></param>
        private void loadFile(string fileName)
        {
            _progressBar.Value = 20;
            _formatpanel.IsEnabled = false;
            _fileCombo.SelectionChanged -= _fileCombo_SelectionChanged;
            Execute(temp + "\\7z.exe", $"x -y \"{fileName}\" -o\"{temp}\" isolinux");
            if (Directory.Exists(temp + "\\isolinux"))
            {
                // This is a linux iso...
                Log($"Detected '{Path.GetFileName(fileName)}' file as linux iso");
                Execute(temp + "\\7z.exe", $"x -y \"{fileName}\" -o\"{temp}\" *.diskdefines");
                if (File.Exists(temp + "\\README.diskdefines"))
                {
                    // This will set proper text in Volume Label TextBox...
                    string firstline = File.ReadAllLines(temp + "\\README.diskdefines")[0];
                    _labelTextBox.Text = Regex.Match(firstline, ".+?(?=\")").Value.Replace("#define DISKNAME", "").Trim() + " " +
                        firstline.Split(' ')[firstline.Split(' ').Length - 1];
                    File.Delete(temp + "\\README.diskdefines");
                }
                else
                {
                    _labelTextBox.Text = Path.GetFileNameWithoutExtension(fileName);
                }
                _progressBar.Value = 100;
                // Since it is linux the Partition Type should be checked as MBR by default...
                _partitionCombo.SelectedIndex = 0;
                // Setting Target system as BIOS or UFEI...
                _targetCombo.Items.Clear();
                _targetCombo.Items.Add("BIOS or UFEI");
                _targetCombo.SelectedIndex = 0;
                DeleteDirectory(temp + "\\isolinux", true);
            }
            else
            {
                /* This is a unix based system could be windows, I am assuming it as windows...
                 * We will use Standard installation for windows instead Windows To Go like RUFUS...
                 */
                Log($"Detected '{Path.GetFileName(fileName)}' file as unix iso");
                // Setting volume label from file name...
                _progressBar.Value = 100;
                _labelTextBox.Text = Path.GetFileNameWithoutExtension(fileName);
                // Unix based system uses GPT partition...
                _partitionCombo.SelectedIndex = 1;
                // Setting Target system as UFEI Only...
                _targetCombo.Items.Clear();
                _targetCombo.Items.Add("UFEI Only");
                _targetCombo.SelectedIndex = 0;
            }
            // Enabling start button...
            _startcancelButton.IsEnabled = true;
            _progressBar.Value = 0;
            _formatpanel.IsEnabled = true;
            _fileCombo.SelectionChanged += _fileCombo_SelectionChanged;
        }
        /// <summary>
        /// This will use to get some default properties for selected USB drive...
        /// </summary>
        private void DetectSize()
        {
            TextBlock label = new TextBlock();
            label = _usbdriveCombo.SelectedItem as TextBlock;
            string driveletter = label.Tag as string;
            foreach (DriveInfo drivet in DriveInfo.GetDrives())
            {
                if (drivet.Name.Contains(driveletter))
                {
                    volumeSize = drivet.TotalSize;
                    volumeLabel = driveletter;
                    return;
                }
            }
        }
        /// <summary>
        /// This method will be use to detect if there is any previously connected USB when application is launched...
        /// </summary>
        private void DetectUSB()
        {
            File.WriteAllText(temp + "\\script.txt", "list volume");
            var output = Regex.Split(Execute("diskpart", $"/s \"{temp + "\\script.txt"}\""), "\r\n|\r|\n");
            foreach (string line in output)
            {
                if (line.Contains("Removable"))
                {
                    // This is a usb drive...
                    string formatted_string = Regex.Replace(line.Trim(), "[ ]{2,}", " ", RegexOptions.None);
                    var driveletter = formatted_string.Split(' ')[2];
                    // Let's get the name and other properties of usb drive...
                    foreach (DriveInfo drive in DriveInfo.GetDrives())
                    {
                        if (drive.Name.Contains(driveletter))
                        {
                            // Add the drive...
                            TextBlock label = new TextBlock();
                            label.Text = drive.VolumeLabel + " (" + drive.Name.Replace("\\", "") + ") [" + SizeSuffix(drive.TotalSize) + "]";
                            label.Tag = drive.Name.Remove(drive.Name.Length - 2);
                            _usbdriveCombo.Items.Add(label);
                            break;
                        }
                    }
                    loadDefaultDrive();
                }
            }
        }
        /// <summary>
        /// Cluster size depends volume size and file system type of USB drive
        /// </summary>
        private void DetectClusterSize()
        {
            // There will be no options for file system FAT16 since it does not support devices more than 2GB
            // In NTFS maximum cluster size is 4 KB which is already included in FAT32...
            _clusterCombo.Items.Clear();
            // Calculate Cluster size for FAT32 partition...
            if (volumeSize > 2147483648) // For over 2GB drive
            {
                _clusterCombo.Items.Add("4096 bytes");
                if (volumeSize < 8589934592) // Make this default...
                {
                    makeDefault();
                }
            }
            if (volumeSize > 8589934592) // For over 8GB drive
            {
                _clusterCombo.Items.Add("8192 bytes");
                if (volumeSize < 17179869184) // Make this default... 
                { 
                    makeDefault();
                }
            }
            if (volumeSize > 17179869184) // For over 16GB drive
            {
                _clusterCombo.Items.Add("16384 bytes");
                if (volumeSize < 34359738368) // Make this default...
                {
                    makeDefault();
                }
            }
            if (volumeSize >= 34359738368)  // For over 32GB drive
            {
                _clusterCombo.Items.Add("32768 bytes");
                makeDefault();
            }
        }
        internal void makeDefault()
        {
            int index = _clusterCombo.Items.Count - 1;
            var size = _clusterCombo.Items[index] as string;
            _clusterCombo.Items[index] = size + " (Default)";
            _clusterCombo.SelectedIndex = index;
        }
        /// <summary>
        /// This will disable all the controls when there is no selected USB drive...
        /// </summary>
        private void Disable(bool all=false)
        {
            _formatpanel.IsEnabled = false;
            _fileCombo.IsEnabled = false;
            if (all)
            {
                _main.IsEnabled = false;
                _main2.IsEnabled = false;
                _usbdriveCombo.IsEnabled = false;
            }
        }
        /// <summary>
        /// This will enable all the controls when there is no selected USB drive...
        /// </summary>
        private void Enable()
        {
            _formatpanel.IsEnabled = true;
            _fileCombo.IsEnabled = true;
            _main.IsEnabled = true;
            _main2.IsEnabled = true;
            _usbdriveCombo.IsEnabled = true;
            _progresslabel.Text = "";
        }
        /// <summary>
        /// This will set the proper index to USBDriveComboBox
        /// </summary>
        private void loadDefaultDrive()
        {
            try
            {
                _usbdriveCombo.SelectedIndex = _usbdriveCombo.Items.Count - 1;
                if (_usbdriveCombo.SelectedIndex <= 0)
                    _startcancelButton.IsEnabled = false;
                DetectSize();
                DetectClusterSize();
            }
            catch (Exception ex) { Log("Error: " + ex.Message, true); }           
        }

        #region Main methods
        static readonly string[] SizeSuffixes =
                 { "bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };
        public void Log(string text, bool error = false)
        {
            _logTextBox.Text += text + "\n";
            if (!error)
                _statusLabel.Text = text;
        }
        public void KillProcess(string name)
        {
            Execute("cmd.exe", "/c taskkill /F /IM /T " + name);
            //Process[] ps = Process.GetProcessesByName(name);

            //foreach (Process p in ps)
            //    p.Kill();
        }
        public static string GetTemporaryDir()
        {
            string temp = Path.GetTempFileName();
            File.Delete(temp);
            Directory.CreateDirectory(temp);
            return temp;
        }
        public void DeleteDirectory(string path, bool delfolder = false)
        {
            BackgroundWorker worker = new BackgroundWorker();
            worker.DoWork += (o, e) =>
            {
                deldir(path, delfolder);
            };
            worker.RunWorkerAsync();
        }
        internal void deldir(String Path, bool delfolder = false)
        {
            System.IO.DirectoryInfo di = new DirectoryInfo(Path);

            foreach (FileInfo file in di.GetFiles())
            {
                file.Delete();
            }
            foreach (DirectoryInfo dir in di.GetDirectories())
            {
                dir.Delete(true);
            }
            if (delfolder)
                Directory.Delete(Path);
        }
        [DllImport("kernel32.dll")]
        static extern uint GetCompressedFileSizeW([In, MarshalAs(UnmanagedType.LPWStr)] string lpFileName,
         [Out, MarshalAs(UnmanagedType.U4)] out uint lpFileSizeHigh);

        [DllImport("kernel32.dll", SetLastError = true, PreserveSig = true)]
        static extern int GetDiskFreeSpaceW([In, MarshalAs(UnmanagedType.LPWStr)] string lpRootPathName,
           out uint lpSectorsPerCluster, out uint lpBytesPerSector, out uint lpNumberOfFreeClusters,
           out uint lpTotalNumberOfClusters);
        public static long GetFileSizeOnDisk(string file)
        {
            FileInfo info = new FileInfo(file);
            uint dummy, sectorsPerCluster, bytesPerSector;
            int result = GetDiskFreeSpaceW(info.Directory.Root.FullName, out sectorsPerCluster, out bytesPerSector, out dummy, out dummy);
            if (result == 0) throw new Win32Exception();
            uint clusterSize = sectorsPerCluster * bytesPerSector;
            uint hosize;
            uint losize = GetCompressedFileSizeW(file, out hosize);
            long size;
            size = (long)hosize << 32 | losize;
            return ((size + clusterSize - 1) / clusterSize) * clusterSize;
        }
        public static void DoEvents()
        {
            Application.Current.Dispatcher.Invoke(DispatcherPriority.Background,
                                                  new Action(delegate { }));
        }
        public void ExtractISO(string filename, string destination)
        {
            _progressBar.IsIndeterminate = false;
            main = new BackgroundWorker();
            main.WorkerSupportsCancellation = true;
            timer = new DispatcherTimer();
            timer.Interval = new TimeSpan(0, 0, 2);
            var mainlength = new FileInfo(filename).Length;
            main.DoWork += (s, e) =>
            {
                Execute7zip(filename, destination);
            };
            timer.Tick += (s, e) =>
            {
                Task<int> io = Task.Factory.StartNew(() => {
                    var length = DirSize(new DirectoryInfo(destination));
                    var calc = Convert.ToInt32(((length * 100) / mainlength));
                    return calc;
                });
                do { DoEvents(); } while (!io.IsCompleted);
                try
                {
                    if (io.Result < 100)
                    {
                        Duration duration = new Duration(TimeSpan.FromSeconds(1));
                        DoubleAnimation doubleanimation = new DoubleAnimation(io.Result, duration);
                        _progressBar.BeginAnimation(ProgressBar.ValueProperty, doubleanimation);
                    }
                    else _progressBar.Value = 100;
                }
                catch {  }
            };
            main.RunWorkerAsync();
            timer.Start();
            while (main.IsBusy)
            {
                DoEvents();
            }
            timer.Stop();
        }
        public string Execute7zip(string filename, string destination)
        {
            string text = null;
            Process p = new Process();
            p.StartInfo.FileName = "cmd.exe";
            p.StartInfo.Arguments = $"/c {temp + "\\7z.exe"} x -y \"{filename}\" -o\"{destination}\"";
            p.StartInfo.RedirectStandardError = true;
            p.StartInfo.RedirectStandardOutput = true;
            p.StartInfo.UseShellExecute = false;
            p.StartInfo.WorkingDirectory = temp;
            p.StartInfo.CreateNoWindow = true;
            p.EnableRaisingEvents = true;
            p.Start();
            do
            {
                DoEvents();
                string output = p.StandardOutput.ReadToEnd();
                text += output;
                string err = p.StandardError.ReadToEnd();
                text += err;
            }
            while (!p.HasExited);
            return text;
        }
        public static string Execute(String Startfunc, String Arguments, String SetWorkingdir = null)
        {
            string text = null;
            Process p = new Process();
            p.StartInfo.FileName = Startfunc;
            p.StartInfo.Arguments = Arguments;
            p.StartInfo.RedirectStandardError = true;
            p.StartInfo.RedirectStandardOutput = true;
            if (SetWorkingdir != null)
                p.StartInfo.WorkingDirectory = SetWorkingdir;
            p.StartInfo.UseShellExecute = false;
            p.StartInfo.CreateNoWindow = true;
            p.EnableRaisingEvents = true;
            p.Start();
            do
            {
                DoEvents();
                string output = p.StandardOutput.ReadToEnd();
                text += output;
                string err = p.StandardError.ReadToEnd();
                text += err;
            }
            while (!p.HasExited);
            return text;
        }
        public static long DirSize(DirectoryInfo d)
        {
            long size = 0;
            // Add file sizes.
            FileInfo[] fis = d.GetFiles();
            foreach (FileInfo fi in fis)
            {
                size += GetFileSizeOnDisk(fi.FullName);
               // size += fi.Length;
            }
            // Add subdirectory sizes.
            DirectoryInfo[] dis = d.GetDirectories();
            foreach (DirectoryInfo di in dis)
            {
                size += DirSize(di);
            }
            return size;
        }
        public static string SizeSuffix(Int64 value, int decimalPlaces = 1)
        {
            if (value < 0) { return "-" + SizeSuffix(-value); }

            int i = 0;
            decimal dValue = (decimal)value;
            while (Math.Round(dValue, decimalPlaces) >= 1000)
            {
                dValue /= 1024;
                i++;
            }

            return string.Format("{0:n" + decimalPlaces + "} {1}", dValue, SizeSuffixes[i]);
        }
        #endregion
    }
}
