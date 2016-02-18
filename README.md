RonnyTrackMate
==============

- Linear Tracker for TrackMate

 1. Link and set a flag for all objects that are sticking more than 80% of the time lapse movie, i.e not moving within a preset radius (Stick radius)
 
 2. Establish a first possible link from an object from the first frame with an object in the second frame within an initial radius
 
 3. Estimate the position of the object in the next frame (3rd) with the obtained vector

 4. Link to an object near to this estimated position within a succeeding radius

 5. Go on to the next frame until the last is reached

- Batch Mode Plug-in to run TrackMate headless from a configuration file (example:Trackmate.properties) which has to placed    in the parent folder of the processed files

- Binary Detector to detect objects from a binary image using the ParticleAnalyzer class from ImageJ

- Export tracks to SQLite

- Export tracks to CSV files
