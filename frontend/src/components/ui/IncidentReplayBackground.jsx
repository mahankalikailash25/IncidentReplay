import React, { useEffect, useRef } from 'react';
import './IncidentReplayBackground.css';

export function IncidentReplayBackground() {
  const containerRef = useRef(null);

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (!containerRef.current) return;
      // Calculate mouse position as a percentage for parallax
      const x = (e.clientX / window.innerWidth - 0.5) * 20;
      const y = (e.clientY / window.innerHeight - 0.5) * 20;
      containerRef.current.style.setProperty('--mouse-x', `${x}px`);
      containerRef.current.style.setProperty('--mouse-y', `${y}px`);
      containerRef.current.style.setProperty('--glow-x', `${e.clientX}px`);
      containerRef.current.style.setProperty('--glow-y', `${e.clientY}px`);
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  return (
    <div className="incident-bg-container" ref={containerRef}>
      {/* Layer 1: Base dark background is handled by body color */}
      
      {/* Layer 2: Faint radial gradients */}
      <div className="incident-bg-gradient"></div>
      
      {/* Layer 6: Mouse reactive ambient glow */}
      <div className="incident-bg-glow"></div>

      {/* Scanning effect */}
      <div className="incident-bg-scan"></div>

      {/* Layer 3: Technical grid */}
      <div className="incident-bg-grid"></div>

      {/* Layer 4 & 5: Network paths, nodes, and packets */}
      <div className="incident-bg-network">
        <svg width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
            <filter id="glow-intense" x="-50%" y="-50%" width="200%" height="200%">
              <feGaussianBlur stdDeviation="6" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
            
            {/* Base Network Paths */}
            <path id="path1" d="M -5%,15% C 15%,15% 30%,45% 45%,45%" fill="none" />
            <path id="path2" d="M 45%,45% C 65%,45% 75%,25% 105%,25%" fill="none" />
            <path id="path3" d="M 45%,45% C 60%,45% 70%,85% 85%,85%" fill="none" />
            <path id="path4" d="M 10%,80% C 25%,80% 35%,55% 45%,45%" fill="none" />
            <path id="path5" d="M 45%,45% C 55%,55% 80%,65% 105%,65%" fill="none" />
            <path id="path-target" d="M 50%,80% C 65%,80% 85%,90% 105%,90%" fill="none" />
            <path id="path-cross1" d="M 10%,30% C 40%,25% 60%,75% 75%,75%" fill="none" />
            <path id="path-cross2" d="M 80%,20% C 70%,40% 30%,60% 15%,85%" fill="none" />
            <path id="path-vertical1" d="M 30%,-5% C 30%,30% 20%,60% 20%,105%" fill="none" />
            <path id="path-vertical2" d="M 80%,-5% C 80%,40% 90%,60% 90%,105%" fill="none" />
            <path id="path-edge1" d="M -5%,60% C 10%,60% 15%,75% 25%,80%" fill="none" />
            <path id="path-edge2" d="M 85%,15% C 90%,15% 95%,5% 105%,10%" fill="none" />
            <path id="path-branch1" d="M 30%,45% C 35%,25% 55%,20% 75%,20%" fill="none" />
          </defs>

          {/* Ambient Drifting Lights */}
          <g className="ambient-drifters">
            <circle cx="20%" cy="30%" r="400" className="drifter drift-1" />
            <circle cx="80%" cy="70%" r="500" className="drifter drift-2" />
            <circle cx="60%" cy="20%" r="300" className="drifter drift-3" />
          </g>

          {/* Target Zone Box */}
          <rect x="80%" y="82%" width="16%" height="16%" rx="8" className="target-zone" />
          <text x="88%" y="90%" className="target-zone-text" textAnchor="middle">TARGET</text>

          {/* Base Network Lines (Static) */}
          <use href="#path1" className="net-line" />
          <use href="#path2" className="net-line opacity-50" />
          <use href="#path3" className="net-line" />
          <use href="#path4" className="net-line opacity-50" />
          <use href="#path5" className="net-line" />
          <use href="#path-target" className="net-line" />
          <use href="#path-cross1" className="net-line opacity-25" />
          <use href="#path-cross2" className="net-line opacity-25" />
          <use href="#path-vertical1" className="net-line opacity-25" />
          <use href="#path-vertical2" className="net-line opacity-25" />
          <use href="#path-edge1" className="net-line opacity-50" />
          <use href="#path-edge2" className="net-line opacity-50" />
          <use href="#path-branch1" className="net-line opacity-50" />

          {/* Data flow lines (Continuous faint dashes) */}
          <use href="#path-cross1" className="net-data-flow" style={{ animationDuration: '18s' }} />
          <use href="#path-cross2" className="net-data-flow" style={{ animationDuration: '22s', animationDirection: 'reverse' }} />
          <use href="#path-vertical1" className="net-data-flow" style={{ animationDuration: '25s' }} />
          <use href="#path-vertical2" className="net-data-flow" style={{ animationDuration: '15s', animationDirection: 'reverse' }} />

          {/* --- MOVING PACKETS --- */}
          {/* Path 1: Normal Request + Replay */}
          <use href="#path1" className="net-packet packet-blue" style={{ animationDuration: '7s', animationDelay: '0s' }} />
          <use href="#path1" className="net-packet packet-replay" style={{ animationDuration: '7s', animationDelay: '1.2s' }} />
          
          {/* Path 2: Responses (Reversed) */}
          <use href="#path2" className="net-packet packet-green reverse" style={{ animationDuration: '6s', animationDelay: '2s' }} />
          <use href="#path2" className="net-packet packet-green reverse" style={{ animationDuration: '6s', animationDelay: '5s' }} />
          
          {/* Path 3: Warning/Amber */}
          <use href="#path3" className="net-packet packet-amber" style={{ animationDuration: '8s', animationDelay: '3s' }} />
          <use href="#path3" className="net-packet packet-amber" style={{ animationDuration: '8s', animationDelay: '7.5s' }} />

          {/* Path 4: Burst Traffic */}
          <use href="#path4" className="net-packet packet-blue" style={{ animationDuration: '10s', animationDelay: '4.0s' }} />
          <use href="#path4" className="net-packet packet-blue" style={{ animationDuration: '10s', animationDelay: '4.2s' }} />
          <use href="#path4" className="net-packet packet-blue" style={{ animationDuration: '10s', animationDelay: '4.4s' }} />
          
          {/* Path 5: Error / Failures */}
          <use href="#path5" className="net-packet packet-red" style={{ animationDuration: '6.5s', animationDelay: '1s' }} />
          <use href="#path5" className="net-packet packet-red" style={{ animationDuration: '6.5s', animationDelay: '6s' }} />
          
          {/* Path Target: Request / Response cycle */}
          <use href="#path-target" className="net-packet packet-cyan" style={{ animationDuration: '4s', animationDelay: '0s' }} />
          <use href="#path-target" className="net-packet packet-cyan" style={{ animationDuration: '4s', animationDelay: '0.8s' }} />
          <use href="#path-target" className="net-packet packet-green reverse" style={{ animationDuration: '5s', animationDelay: '2.5s' }} />
          
          {/* Edge / Branch Packets */}
          <use href="#path-edge1" className="net-packet packet-blue" style={{ animationDuration: '5s', animationDelay: '3s' }} />
          <use href="#path-branch1" className="net-packet packet-blue" style={{ animationDuration: '8s', animationDelay: '2s' }} />

          {/* Nodes */}
          <g className="net-nodes">
            {/* Start Nodes */}
            <circle cx="10%" cy="15%" r="3" className="node node-primary" />
            <circle cx="10%" cy="80%" r="3" className="node node-primary heartbeat-node" style={{ animationDelay: '1s' }} />
            <circle cx="20%" cy="60%" r="3" className="node node-primary opacity-50" />
            
            {/* Cross Nodes */}
            <circle cx="10%" cy="30%" r="3" className="node node-primary" />
            <circle cx="75%" cy="75%" r="3" className="node node-warning" />
            
            {/* Central Service Node */}
            <circle cx="45%" cy="45%" r="5" className="node node-primary heartbeat-node" />
            <circle cx="45%" cy="45%" r="18" className="node-pulse" />
            
            {/* End Nodes */}
            <circle cx="95%" cy="25%" r="3" className="node node-success heartbeat-node" style={{ animationDelay: '2s' }} />
            <circle cx="85%" cy="85%" r="3" className="node node-warning" />
            
            {/* Target Entry Node */}
            <circle cx="50%" cy="80%" r="4" className="node node-primary" />
            <circle cx="50%" cy="80%" r="12" className="node-pulse" style={{ animationDelay: '2.5s' }} />
            
            {/* Incident Signal Node */}
            <circle cx="90%" cy="65%" r="14" className="node-pulse pulse-error" style={{ animationDelay: '1s' }} />
            <circle cx="90%" cy="65%" r="3" className="node node-error heartbeat-error" />
            
            {/* Edges / Branches */}
            <circle cx="75%" cy="20%" r="2" className="node node-primary" />
            <circle cx="30%" cy="45%" r="2" className="node node-primary opacity-50" />
          </g>
        </svg>
      </div>
    </div>
  );
}
