using System.Net;
using Xamarin.Forms;
[assembly: Dependency(typeof(App1.Droid.WifiIp))]
namespace App1.Droid
{
    public class WifiIp : IWifiIp
    {
        public string GetWifiIp()
        {
            var adresses = Dns.GetHostAddresses(Dns.GetHostName());
            var ip  = adresses != null && adresses[0] != null ? adresses[0].ToString() : null;
            return ip;
        }
    }
}