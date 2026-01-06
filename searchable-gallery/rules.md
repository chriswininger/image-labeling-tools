# searchable-gallery Project Rules

## Project Overview
This is an Electron desktop application built with React, TypeScript, Redux Toolkit, and Vite. It's a searchable image gallery using a SQLite database.

## Architecture

### Electron Process Model
- **Main Process** (`src/main.ts`): Handles native APIs, database operations, IPC handlers, and file system access
- **Preload Script** (`src/preload.ts`): Exposes safe APIs to renderer via `contextBridge`
- **Renderer Process** (`src/renderer.tsx`): React application entry point

### IPC Communication Pattern
- Use `ipcMain.handle()` in main process for async operations
- Use `ipcRenderer.invoke()` exposed through `window.electronAPI` in renderer
- Always expose APIs through preload script with `contextBridge.exposeInMainWorld()`
- Never enable `nodeIntegration` - always use `contextIsolation: true`

## File Structure Conventions

### Components
- Each component lives in its own folder under `src/components/`
- Folder contains the `.tsx` file and corresponding `.css` file with matching names
- Example: `Gallery/Gallery.tsx` and `Gallery/Gallery.css`

### CSS Imports
- Import CSS files directly into component files: `import './ComponentName.css'`
- Use BEM-like naming: `.component-name`, `.component-name-element`, `.component-name-modifier`

### Types
- Store TypeScript interfaces in `src/types/` directory
- Use `.d.ts` files for type declarations and global augmentations
- Export interfaces individually: `export interface TypeName { ... }`

## State Management (Redux Toolkit)

### Store Structure
- Store configuration in `src/store/store.ts`
- Export `RootState` and `AppDispatch` types from store
- One slice per feature in `src/store/[feature]Slice.ts`

### Custom Hooks
- Use typed hooks from `src/store/hooks.ts`:
    - `useAppDispatch` instead of `useDispatch`
    - `useAppSelector` instead of `useSelector`

### Async Thunks
- Use `createAsyncThunk` for async operations
- Always check for `window.electronAPI` availability before calling
- Handle loading states: `'idle' | 'loading' | 'succeeded' | 'failed'`


## React Conventions

### Function Components
- Use function declarations: `function ComponentName() { }`
- Export as default: `export default ComponentName;`

### Helper Function Placement
**IMPORTANT: Place all helper functions AFTER the return statement, not before.**

Use `function` declarations (not arrow functions) for helpers so they are hoisted.

❌ **Don't do this:**
```
const getMessage = () => return 'my message';

return <div>{getMessage()}</div>
```

✅ **Do this instead:**
```
return <div>{getMessage()}</div>

function getMessage() {
   return 'my message';
}
```
