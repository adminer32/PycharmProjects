import asyncio
import cv2
import base64
import os
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from ultralytics import YOLO
import uvicorn

app = FastAPI()


current_dir = os.path.dirname(os.path.abspath(__file__))
VIDEO_PATH = os.path.join(current_dir, "public", "aaaaa.mp4")

@app.websocket("/ws/analysis")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    
    cap = cv2.VideoCapture(VIDEO_PATH)

    frame_count = 0
    
    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                
            frame_count += 1
            if frame_count % FRAME_SKIP != 0:
                continue

            results = model.predict(frame, classes=[0], verbose=False)
            annotated_frame = results[0].plot()
            
            

            _, buffer = cv2.imencode('.jpg', annotated_frame, [cv2.IMWRITE_JPEG_QUALITY, 60])
            frame_b64 = base64.b64encode(buffer).decode('utf-8')
            
            await websocket.send_json({
                "status": "running",
                "image": frame_b64,
                "attendanceRate": attendance_rate,
                "engagement": engagement
            })
            
            
    except WebSocketDisconnect:
    except Exception as e:
    finally:
        cap.release()
        
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)