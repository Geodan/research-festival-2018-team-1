using UnityEngine;

public class AgentInteraction : MonoBehaviour
{
    public GameObject agentObject;
    public GameObject agentCanvas;
    public GameObject rayCastCatcher;

    private bool canvasActive = false;

    public void EnableCanvas()
    {
        canvasActive = true;
        SetCanvas();
    }

    public void DisableCanvas()
    {
        canvasActive = false;
        SetCanvas();
    }

    void SetCanvas()
    {
        agentCanvas.SetActive(canvasActive);
        rayCastCatcher.SetActive(canvasActive);
    }
}
