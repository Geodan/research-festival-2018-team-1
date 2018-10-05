using Xamarin.Essentials;
using Xamarin.Forms;

namespace App1
{
    public partial class MainPage : ContentPage
    {
        private EventSystem _eventSystem;

        public MainPage(EventSystem eventSystem)
        {
            _eventSystem = eventSystem;
            InitializeComponent();
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            _eventSystem.Start();

            Compass.Start(SensorSpeed.Normal);
            Compass.ReadingChanged += Compass_ReadingChanged;

            Accelerometer.Start(SensorSpeed.Normal);
            Accelerometer.ReadingChanged += Accelerometer_ReadingChanged;

            TxtName.TextChanged += TxtName_TextChanged;
            _eventSystem.Name = TxtName.Text;
        }

        private void TxtName_TextChanged(object sender, TextChangedEventArgs e)
        {
            _eventSystem.Name = TxtName.Text;
        }

        private void Compass_ReadingChanged(object sender, CompassChangedEventArgs e)
        {
            var data = e.Reading;
            _eventSystem.CompassEvent(data.HeadingMagneticNorth);
        }

        private void Accelerometer_ReadingChanged(object sender, AccelerometerChangedEventArgs e)
        {
            var data = e.Reading;
            _eventSystem.AccelerometerEvent(data.Acceleration.X, data.Acceleration.Y, data.Acceleration.Z);
        }
    }
}
