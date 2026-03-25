import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

const Home = () => {
    return (
        <div style={{ border: '2px dashed blue', padding: '20px', margin: '20px' }}>
            <h2>Hello from Module APIM (Remote)</h2>
            <p>This is a microfrontend exposed via Module Federation 2.0!</p>
        </div>
    );
};

export const AppRoutes = () => {
    return (
        <Routes>
            <Route path="/" Component={Home} />
            <Route path="*" Component={Home} />
        </Routes>
    );
};

const App = () => {
    return (
        <BrowserRouter basename="/">
            <AppRoutes />
        </BrowserRouter>
    );
};

export default App;
