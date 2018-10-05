using UnityEngine;

public class TargetLocation : MonoBehaviour
{
    public GameObject targetPosObj;

    private bool targetExists = false;

    public void SetNewTargetLoc(Vector3 targetPos)
    {
        if (!targetExists)
        {
            if (GameObject.Find("TargetPos") != null)
            {
                GameObject.Find("TargetPos").transform.position = targetPos;
            }
            else
            {
                Instantiate(targetPosObj, targetPos, Quaternion.identity);
            }

            targetExists = true;
        }
    }

    public void DestroyTargetObject(GameObject other)
    {
        Destroy(other);
        targetExists = false;
    }
}
