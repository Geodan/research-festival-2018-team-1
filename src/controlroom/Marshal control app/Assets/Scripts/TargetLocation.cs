using UnityEngine;

public class TargetLocation : MonoBehaviour
{
    public GameObject targetPosObj;

    private GameObject targetInstance;

    private bool targetExists = false;

    public void SetNewTargetLoc(Vector3 targetPos)
    {
        if (targetExists)
        {
            if (targetInstance != null)
            {
                targetInstance.transform.position = targetPos;
            }
        }
        else
        {
            GameObject instance = Instantiate(targetPosObj, targetPos, Quaternion.identity);
            targetInstance = instance;
            targetExists = true;
        }
    }

    public void DestroyTargetObject(GameObject other)
    {
        Destroy(other);
        targetExists = false;
    }
}
