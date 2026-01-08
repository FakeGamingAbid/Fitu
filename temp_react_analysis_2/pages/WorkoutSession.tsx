import React, { useRef, useState, useEffect } from 'react';
import Webcam from 'react-webcam';
import { useNavigate, useParams } from 'react-router-dom';
import { useStore } from '../store';
import { ExerciseType } from '../types';
import { WifiOff, RefreshCw } from 'lucide-react';

// Use global variables loaded via script tags in index.html
declare global {
  interface Window {
    tf: any;
    poseDetection: any;
  }
}

const WorkoutSession: React.FC = () => {
  const navigate = useNavigate();
  const { type } = useParams<{ type: string }>();
  // Default to squat if invalid type provided
  const exerciseType = (['squat', 'pushup', 'jumping_jack'].includes(type || '') ? type : 'squat') as ExerciseType;

  const webcamRef = useRef<Webcam>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [detector, setDetector] = useState<any>(null);
  const [reps, setReps] = useState(0);
  const [feedback, setFeedback] = useState("Get in position");
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  
  // Use refs for loop state to avoid closure staleness
  const exerciseStateRef = useRef<'up' | 'down'>('up');
  
  const addWorkout = useStore(state => state.addWorkout);
  const requestRef = useRef<number>(0);

  // Initialize state based on exercise type
  useEffect(() => {
    // Squats/Pushups start 'up' (extended)
    // Jumping Jacks start 'down' (arms down)
    if (exerciseType === 'jumping_jack') {
      exerciseStateRef.current = 'down';
    } else {
      exerciseStateRef.current = 'up';
    }
  }, [exerciseType]);

  const initTF = async () => {
    setIsLoading(true);
    setLoadError(null);
    
    try {
      const tf = window.tf;
      const poseDetection = window.poseDetection;
      
      if (!tf || !poseDetection) {
        throw new Error("TensorFlow.js not loaded. Check your connection.");
      }

      await tf.ready();
      
      // Optimization for mobile WebGL stability
      tf.env().set('WEBGL_DELETE_TEXTURE_THRESHOLD', 0);

      const model = poseDetection.SupportedModels.MoveNet;
      // Use Thunder for better accuracy (slower but worth it for form correction)
      const detectorConfig = { modelType: poseDetection.movenet.modelType.SINGLEPOSE_THUNDER };
      
      const detector = await poseDetection.createDetector(model, detectorConfig);
      setDetector(detector);
      setIsLoading(false);
    } catch (err: any) {
      console.error("Model loading error:", err);
      setIsLoading(false);
      
      if (!navigator.onLine) {
        setLoadError("You are offline and the AI model hasn't been cached yet. Please connect to the internet for the first run.");
      } else {
        setLoadError("Failed to load AI Vision. Please refresh and try again.");
      }
    }
  };

  useEffect(() => {
    initTF();
  }, []);

  const getAngle = (a: any, b: any, c: any) => {
    if (!a || !b || !c) return 0;
    const radians = Math.atan2(c.y - b.y, c.x - b.x) - Math.atan2(a.y - b.y, a.x - b.x);
    let angle = Math.abs(radians * 180.0 / Math.PI);
    if (angle > 180.0) angle = 360 - angle;
    return angle;
  };

  const drawSkeleton = (keypoints: any[], ctx: CanvasRenderingContext2D) => {
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    
    // Config matching the reference video style but with Primary Orange
    const skeletonColor = '#F6822B'; 
    ctx.lineWidth = 4;
    ctx.strokeStyle = skeletonColor;
    ctx.fillStyle = skeletonColor;

    // Define connections including "box" frame (shoulders-hips)
    const connections = [
      ['left_shoulder', 'right_shoulder'],
      ['left_shoulder', 'left_elbow'], ['left_elbow', 'left_wrist'],
      ['right_shoulder', 'right_elbow'], ['right_elbow', 'right_wrist'],
      ['left_shoulder', 'left_hip'], ['right_shoulder', 'right_hip'],
      ['left_hip', 'right_hip'],
      ['left_hip', 'left_knee'], ['left_knee', 'left_ankle'],
      ['right_hip', 'right_knee'], ['right_knee', 'right_ankle'],
      // Add nose-to-shoulders to visualize head position roughly
      ['nose', 'left_shoulder'], ['nose', 'right_shoulder']
    ];

    // Draw Lines
    connections.forEach(([p1, p2]) => {
      const kp1 = keypoints.find(k => k.name === p1);
      const kp2 = keypoints.find(k => k.name === p2);
      if (kp1 && kp2 && kp1.score > 0.3 && kp2.score > 0.3) {
        ctx.beginPath();
        ctx.moveTo(kp1.x, kp1.y);
        ctx.lineTo(kp2.x, kp2.y);
        ctx.stroke();
      }
    });

    // Draw Joints
    keypoints.forEach((kp) => {
      // Only draw main joints, ignore eyes/ears to keep it clean, but keep nose
      const validPoints = ['nose', 'left_shoulder', 'right_shoulder', 'left_elbow', 'right_elbow', 'left_wrist', 'right_wrist', 'left_hip', 'right_hip', 'left_knee', 'right_knee', 'left_ankle', 'right_ankle'];
      
      if (validPoints.includes(kp.name) && kp.score && kp.score > 0.3) {
        // Outer Color Circle
        ctx.beginPath();
        ctx.arc(kp.x, kp.y, 6, 0, 2 * Math.PI);
        ctx.fillStyle = skeletonColor;
        ctx.fill();
        
        // Inner White Dot (Joint look)
        ctx.beginPath();
        ctx.arc(kp.x, kp.y, 3, 0, 2 * Math.PI);
        ctx.fillStyle = '#FFFFFF';
        ctx.fill();
      }
    });
  };

  const detectPose = async () => {
    if (detector && webcamRef.current && webcamRef.current.video && webcamRef.current.video.readyState === 4) {
      const video = webcamRef.current.video;
      const videoWidth = video.videoWidth;
      const videoHeight = video.videoHeight;

      if (canvasRef.current) {
        canvasRef.current.width = videoWidth;
        canvasRef.current.height = videoHeight;
      }

      try {
        const poses = await detector.estimatePoses(video);
        
        if (poses.length > 0) {
          const keypoints = poses[0].keypoints;
          const ctx = canvasRef.current?.getContext('2d');
          if (ctx) drawSkeleton(keypoints, ctx);

          // Logic based on exercise type
          if (exerciseType === 'squat') {
            const leftHip = keypoints.find((k: any) => k.name === 'left_hip');
            const leftKnee = keypoints.find((k: any) => k.name === 'left_knee');
            const leftAnkle = keypoints.find((k: any) => k.name === 'left_ankle');

            if (leftHip?.score > 0.3 && leftKnee?.score > 0.3 && leftAnkle?.score > 0.3) {
              const angle = getAngle(leftHip, leftKnee, leftAnkle);
              
              if (angle < 100) {
                  if (exerciseStateRef.current === 'up') {
                    setFeedback("Good depth!");
                    exerciseStateRef.current = 'down';
                  }
              } else if (angle > 160) {
                  if (exerciseStateRef.current === 'down') {
                    setReps(r => r + 1);
                    setFeedback("Great rep!");
                    exerciseStateRef.current = 'up';
                  } else {
                    // Only show guidance if not in transition
                    if (exerciseStateRef.current === 'up') setFeedback("Go lower");
                  }
              }
            } else {
                setFeedback("Whole body not visible");
            }
          } else if (exerciseType === 'pushup') {
            const shoulder = keypoints.find((k: any) => k.name === 'left_shoulder');
            const elbow = keypoints.find((k: any) => k.name === 'left_elbow');
            const wrist = keypoints.find((k: any) => k.name === 'left_wrist');

            if (shoulder?.score > 0.3 && elbow?.score > 0.3 && wrist?.score > 0.3) {
               const angle = getAngle(shoulder, elbow, wrist);
               if (angle < 90) {
                  if (exerciseStateRef.current === 'up') {
                     exerciseStateRef.current = 'down';
                     setFeedback("Push up!");
                  }
               } else if (angle > 160) {
                  if (exerciseStateRef.current === 'down') {
                     setReps(r => r + 1);
                     exerciseStateRef.current = 'up';
                     setFeedback("Good form");
                  }
               }
            }
          } else if (exerciseType === 'jumping_jack') {
            const lWrist = keypoints.find((k: any) => k.name === 'left_wrist');
            const rWrist = keypoints.find((k: any) => k.name === 'right_wrist');
            const lShoulder = keypoints.find((k: any) => k.name === 'left_shoulder');
            const rShoulder = keypoints.find((k: any) => k.name === 'right_shoulder');
            
            if (lWrist?.score > 0.3 && rWrist?.score > 0.3 && lShoulder?.score > 0.3) {
                // Check if wrists are above shoulders (Arms Up)
                const isArmsUp = lWrist.y < lShoulder.y && rWrist.y < rShoulder.y;
                // Check if wrists are below shoulders (Arms Down)
                const isArmsDown = lWrist.y > lShoulder.y && rWrist.y > rShoulder.y;

                if (isArmsUp) {
                    if (exerciseStateRef.current === 'down') {
                        setFeedback("Good extension!");
                        exerciseStateRef.current = 'up';
                    }
                } else if (isArmsDown) {
                    if (exerciseStateRef.current === 'up') {
                        setReps(r => r + 1);
                        setFeedback("Good!");
                        exerciseStateRef.current = 'down';
                    }
                }
            } else {
                setFeedback("Visible upper body needed");
            }
          } else {
            setFeedback("Maintain form");
          }
        }
      } catch (e) {
        console.warn("Pose estimation error:", e);
      }
    }
    requestRef.current = requestAnimationFrame(detectPose);
  };

  useEffect(() => {
    if (!isLoading && !loadError && detector) {
      requestRef.current = requestAnimationFrame(detectPose);
    }
    return () => {
      if (requestRef.current) cancelAnimationFrame(requestRef.current);
    };
  }, [detector, exerciseType, isLoading, loadError]); // Removed exerciseStateRef as it's a ref

  const handleStop = () => {
    if (reps > 0) {
      addWorkout({
        id: Date.now().toString(),
        type: exerciseType,
        reps,
        duration: 0, 
        calories: reps * 0.5,
        timestamp: Date.now(),
        formScore: 90
      });
    }
    navigate('/coach'); // Return to coach dashboard
  };

  if (loadError) {
    return (
       <div className="fixed inset-0 bg-[#0A0A0F] z-50 flex flex-col items-center justify-center p-6 text-center">
          <div className="w-20 h-20 bg-red-500/10 rounded-full flex items-center justify-center mb-6">
            <WifiOff className="text-red-500" size={32} />
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Setup Failed</h2>
          <p className="text-white/50 mb-8 max-w-xs mx-auto">{loadError}</p>
          
          <div className="flex flex-col gap-3 w-full max-w-xs">
            <button 
              onClick={() => initTF()}
              className="bg-[#F6822B] text-white px-6 py-4 rounded-xl font-bold flex items-center justify-center gap-2 active:scale-95 transition-transform"
            >
              <RefreshCw size={20} /> Retry Connection
            </button>
            <button 
              onClick={() => navigate('/coach')}
              className="bg-white/10 text-white px-6 py-4 rounded-xl font-bold active:scale-95 transition-transform"
            >
              Go Back
            </button>
          </div>
       </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black z-50 flex flex-col">
      {/* Top Header */}
      <div className="absolute top-0 left-0 right-0 p-4 z-20 flex justify-between items-center bg-gradient-to-b from-black/60 to-transparent">
        <h1 className="text-xl font-bold text-[#F6822B] italic tracking-tighter drop-shadow-md">Fitu</h1>
        <button 
          onClick={handleStop}
          className="bg-red-600 text-white px-4 py-2 rounded-lg font-bold text-sm uppercase tracking-wide shadow-lg hover:bg-red-700 active:scale-95 transition-transform"
        >
          Stop Tracking
        </button>
      </div>

      {/* Main Video Area */}
      <div className="flex-1 relative bg-[#0A0A0F]">
        {isLoading && (
          <div className="absolute inset-0 flex items-center justify-center z-10">
            <div className="flex flex-col items-center gap-4">
               <div className="w-10 h-10 border-4 border-[#F6822B] border-t-transparent rounded-full animate-spin"></div>
               <p className="text-white/50 text-sm">Initializing Vision Engine...</p>
               <p className="text-white/30 text-xs">First load requires internet</p>
            </div>
          </div>
        )}
        <Webcam
          ref={webcamRef}
          className="absolute inset-0 w-full h-full object-cover"
          mirrored
          videoConstraints={{ facingMode: "user" }}
        />
        <canvas
          ref={canvasRef}
          className="absolute inset-0 w-full h-full object-cover scale-x-[-1]"
        />
      </div>

      {/* Bottom Info Bar - Matching the Reference Video Style */}
      <div className="bg-white p-6 pb-8 z-20 grid grid-cols-3 divide-x divide-gray-200 shadow-[0_-4px_20px_rgba(0,0,0,0.2)]">
        {/* Reps Section */}
        <div className="flex flex-col items-center justify-center text-center px-2">
          <span className="text-5xl font-bold text-[#F6822B] font-sans leading-none">{reps}</span>
          <span className="text-gray-500 text-[10px] font-extrabold uppercase mt-1 tracking-wider">Reps</span>
        </div>

        {/* Exercise Section */}
        <div className="flex flex-col items-center justify-center text-center px-2">
          <span className="text-gray-400 text-[10px] font-extrabold uppercase mb-1 tracking-wider">Exercise</span>
          <span className="text-gray-800 font-bold text-lg leading-tight capitalize">{exerciseType.replace('_', ' ')}</span>
        </div>

        {/* Form Section */}
        <div className="flex flex-col items-center justify-center text-center px-2">
          <span className="text-gray-400 text-[10px] font-extrabold uppercase mb-1 tracking-wider">Form</span>
          <span className="text-gray-800 font-medium text-sm leading-tight line-clamp-2">{feedback}</span>
        </div>
      </div>
    </div>
  );
};

export default WorkoutSession;