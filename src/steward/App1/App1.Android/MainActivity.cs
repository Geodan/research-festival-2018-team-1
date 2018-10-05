using System;

using Android.App;
using Android.Content.PM;
using Android.Runtime;
using Android.OS;
using Android.Hardware;
using Android.Widget;

namespace App1.Droid
{
    [Activity(Label = "App1", Icon = "@mipmap/icon", Theme = "@style/MainTheme", MainLauncher = true, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation)]
    public class MainActivity : global::Xamarin.Forms.Platform.Android.FormsAppCompatActivity, ISensorEventListener
    {
        private EventSystem _eventSystem;
        private SensorManager _senMan;
        static readonly object _syncLock = new object();

        public void OnAccuracyChanged(Sensor sensor, [GeneratedEnum] SensorStatus accuracy)
        {
        }

        private int step = 0;

        public void OnSensorChanged(SensorEvent e)
        {
            lock (_syncLock)
            {
                if (e.Sensor.Type == SensorType.StepCounter)
                {
                    step++;
                    _eventSystem.StepEvent(e.Values[0]);
                }
            }
        }

        protected override void OnCreate(Bundle savedInstanceState)
        {
            TabLayoutResource = Resource.Layout.Tabbar;
            ToolbarResource = Resource.Layout.Toolbar;

            base.OnCreate(savedInstanceState);
            global::Xamarin.Forms.Forms.Init(this, savedInstanceState);

            var app = new App();
            LoadApplication(app);

            _eventSystem = app.EventSystem;

            _senMan = (SensorManager)GetSystemService(SensorService);
            
            Sensor sen = _senMan.GetDefaultSensor(SensorType.StepCounter);
            _senMan.RegisterListener(this, sen, SensorDelay.Fastest);
        }
    }
}