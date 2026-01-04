import { useEffect } from 'react';
import { Provider } from 'react-redux';
import { store } from '../store/store';
import Gallery from './Gallery';

function App() {
  return (
    <Provider store={store}>
      <div className="app">
        <h1>Image Gallery</h1>
        <Gallery />
      </div>
    </Provider>
  );
}

export default App;

