﻿using System;
using System.Text;
using UnityEngine;
using uPLibrary.Networking.M2Mqtt;
using uPLibrary.Networking.M2Mqtt.Messages;

public class TestScript : MonoBehaviour {

	// Use this for initialization
	void Start () {
        var broker = "broker.hivemq.com";
        var port = 1883;
        var topic = "arena/test";
        var message = "berthoho67890";

        var client = new MqttClient(broker);
        client.MqttMsgPublishReceived += client_MqttMsgPublishReceived;
        var clientId = Guid.NewGuid().ToString();
        client.Connect(clientId);
        client.Subscribe(new[] { topic }, new[] { MqttMsgBase.QOS_LEVEL_EXACTLY_ONCE });
        client.Publish(topic, Encoding.Default.GetBytes(message));
    }

    private static void client_MqttMsgPublishReceived(object sender, MqttMsgPublishEventArgs e)
    {
        var str = Encoding.Default.GetString(e.Message);
        Console.WriteLine("result: " + str);
    }
}
