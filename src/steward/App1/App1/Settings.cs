using Xamarin.Essentials;

namespace App1
{
    public class Settings
    {
        private const string _stewardNameSettings = "stewardName";

        public static string StewardName {
            get{
                return Preferences.Get(_stewardNameSettings, "steward1");
            } set{
                Preferences.Set(_stewardNameSettings, value);
            }
        }
    }
}
