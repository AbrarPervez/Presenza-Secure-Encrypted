📸 PresenzaSecureEncrypted
AI-Powered Face Attendance with Liveness & Lip-Sync Verification
   
PresenzaSecureEncrypted is a next-generation attendance system that replaces traditional logs with high-security facial biometric verification. By combining CameraX with 3D Face Mesh, the app ensures that the person behind the lens is real, active, and verified.
1. 👄 Lip-Sync Liveness Detection
To prevent spoofing via photos or pre-recorded videos, the app displays a random 4-digit code. The system uses the camera to track 468-point Face Mesh landmarks, verifying that the user's lip movements match the patterns of live speech.

2. 🎯 MobileFaceNet Recognition
Behind the camera, the system runs the mobilefacenet.tflite model. It crops the face in real-time and converts it into a unique 128D/512D mathematical embedding for high-speed matching.

3. 🔒 Encrypted Biometric Vault
Data privacy is our priority. All face embeddings are stored in a Room Database secured with AES-256 encryption. The biometric "fingerprint" never leaves the device's local storage.

4. 🎞️ High-Performance Stream
Leverages CameraX Analysis Use-Case to process frames at 30FPS, providing a smooth, lag-free experience for the user while performing heavy ML computations in the background.

🛠️ Technical Stack

UI/UX: Material Design 3 & ViewBinding

Vision: Google ML Kit: Face Detection & 3D Mesh

CameraX: Lifecycle-aware camera control  

Intelligence: TensorFlow Lite: MobileFaceNet for sub-second recognition

Storage: Room DB: Local storage for User.kt entities (Roll No, Name, Section, Embeddings)




🤝 Contributing
Found a bug in the camera logic? Open a Pull Request! We are always looking to improve our recognition accuracy and liveness algorithms.

Developed by Abrar Pervez,Vaibhav Mahesh Kudal and Raj Sharma

Verification at the speed of sight.
