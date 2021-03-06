package com.example.lize.models;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.lize.adapters.DatabaseAdapter;
import com.example.lize.data.Ambito;
import com.example.lize.data.Folder;
import com.example.lize.data.Note;
import com.example.lize.data.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Date;


public class MainViewModel extends ViewModel{

    private static final String TAG = "MainViewModel";

    private final MutableLiveData<User> mUserSelected;
    private final MutableLiveData<Ambito> mAmbitoSelected;
    private final MutableLiveData<Folder> mFolderSelected;
    private final MutableLiveData<Note> mNoteSelected;
    private final MutableLiveData<String> mToast;
    private final MutableLiveData<Boolean> mViewUpdated;

    private final DatabaseAdapter databaseAdapter;
    private final DocumentManager documentManager;

    /**
     * Constructor de la clase
     */
    public MainViewModel() {
        mUserSelected = new MutableLiveData<>();
        mAmbitoSelected = new MutableLiveData<>();
        mFolderSelected = new MutableLiveData<>();
        mNoteSelected = new MutableLiveData<>();
        mToast = new MutableLiveData<>();
        mViewUpdated = new MutableLiveData<>();
        mViewUpdated.setValue(false);

        // Enlazamos con la base de datos, reconstruyendo la jerarquía del modelo a partir del Usuario Registrado
        this.databaseAdapter = DatabaseAdapter.getInstance();
        this.documentManager = DocumentManager.getInstance();

        databaseAdapter.setLoaderListener(new UserBuilder());
        databaseAdapter.initFireBase();
        databaseAdapter.getUser();
    }

    /**
     * Metodo para conseguir el User actual
     * @return User actual
     */
    public MutableLiveData<User> getUserSelected() { return mUserSelected; }

    /**
     * Metodo para conseguir el Ambito actual
     * @return Ambito actual
     */
    public MutableLiveData<Ambito> getAmbitoSelected() {
        return mAmbitoSelected;
    }

    /**
     * Metodo para conseguir la Carpeta seleccionada
     * @return Carpeta seleccionada
     */
    public MutableLiveData<Folder> getFolderSelected() { return mFolderSelected; }

    /**
     * Metodo para conseguir la Nota seleccionada
     * @return Nota seleccionada
     */
    public MutableLiveData<Note> getNoteSelected() { return mNoteSelected; }

    /**
     * Metodo para conseguir el toast actual
     * @return Toast actual
     */
    public MutableLiveData<String> getToast() {
        return mToast;
    }

    /**
     * Metodo para conseguir el View updated actual
     * @return View updated actual
     */
    public MutableLiveData<Boolean> getViewUpdate(){ return mViewUpdated; }


    //***************************
    //      SELECT & DESELECT
    //***************************

    /**
     * Selecciona un Ámbito del Usuario logueado mUserSelected. Antes de nada, marcamos que la Vista
     * se debe refrescar para así evitar dobles llamadas a los observadores.
     * @param ambitoName Ambito seleccionado
     * @throws NullPointerException Si el Usuario logueado no ha sido correctamente cargado de DB.
     */
    public void selectAmbito(String ambitoName) {
        try {
            for (Ambito ambito : mUserSelected.getValue().getAmbitos()) {
                if (ambito.getName().equals(ambitoName)) {
                    mViewUpdated.setValue(false);
                    setToast("Ambito " + ambitoName + " selected.");
                    mAmbitoSelected.setValue(ambito);
                    mFolderSelected.setValue(null);
                    return;
                }
            }
            Log.w(TAG, "Failed to select ambito " + ambitoName + ": invalid ID.");
        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to select ambito " + ambitoName + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    /**
     * Selecciona una Carpeta del Ámbito actual mAmbitoSelected
     * @param folderName Carpeta Seleccionada
     * @throws NullPointerException Si el Ámbito actual no ha sido correctamente seleccionado.
     */
    public void selectFolder(String folderName) {
        try {
            Folder folder = mAmbitoSelected.getValue().getFolder(folderName);
            if (folder != null) {
                setToast("Folder " + folderName + " selected.");
                mFolderSelected.setValue(folder);
            } else Log.w(TAG, "Failed to select folder " + folder.getName()  + ": invalid Name.");

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to select folder " + folderName + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }


    /**
     * Deselecciona la Carpeta mFolderSelected del Ámbito actual, actualizando su lista de notas general.
     */
    public void deselectFolder() {
        try {
            Folder selectedFolder = mFolderSelected.getValue();
            if (selectedFolder != null) {
                setToast("Folder " + selectedFolder.getName() + " deselected.");
                mFolderSelected.setValue(null);
            } else Log.w(TAG, "Failed to deselect current Folder: no Folder selected");

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to deselect current folder: null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }


    /**
     * Selecciona una Nota del Ámbito actual mAmbitoSelected.
     * @param noteID ID de la Nota seleccionada.
     * @throws NullPointerException Si el Ámbito actual no ha sido correctamente seleccionado.
     */
    public void selectNote(String noteID) {
        try {
            for (Note note :mAmbitoSelected.getValue().getNotes()) {
                if (note.getSelfID().equals(noteID)) {
                    setToast("Note " + note.getTitle() + " selected.");
                    mNoteSelected.setValue(note);
                    return;
                }
            }
            Log.w(TAG, "Failed to select note " + noteID + ": invalid ID.");

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to select note " + noteID + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    //*****************
    //      ADD's
    //*****************

    /**
     * Añadimos un nuevo Ámbito a la lista de Ámbitos del Usuario logueado.
     * @param ambitoName Nombre del Ámbito a crear. Debe ser distinto a los demás Ámbitos del Usuario.
     * @param ambitoColor Color del Ámbito a crear. Debe ser distinto a los demás Ámbitos del Usuario.
     * @throws NullPointerException Si el Usuario logueado no ha sido correctamente seleccionado.
     */
    public void addAmbito(String ambitoName, int ambitoColor) {
        try {
            for (Ambito ambito : mUserSelected.getValue().getAmbitos())
                if (ambito.getName().equals(ambitoName)) {
                    Log.w(TAG, "Failed to create ambito " + ambitoName + ": ambito already exists. ");
                    return;
                }
            Ambito newAmbito = new Ambito(ambitoName, ambitoColor);     // Creamos un nuevo Ámbito
            mUserSelected.getValue().addAmbito(newAmbito);              // Añadimos ese Ámbito al Usuario registrado
            mUserSelected.setValue(mUserSelected.getValue());           // Actualizamos la colección de Ámbitos del Usuario registrado
            databaseAdapter.saveAmbito(newAmbito);                      // Guardamos el Ámbito en DB
            setToast("Ambito " + ambitoName + " correctly created.");   // Creamos Toast informativo

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to add ambito " + ambitoName + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    /**
     * Añadimos una nueva Carpeta a la lista de Carpetas del Ámbito actual.
     * @param folderName Nombre de la carpeta a crear. Debe ser no nula y distinta a las demás Carpetas del Ámbito.
     * @throws NullPointerException Si el Ámbito actual no ha sido correctamente seleccionado.
     */
    public void addFolder(String folderName) {
        try {
            if (folderName == null || folderName.length() == 0) {
                Log.w(TAG, "Failed to create folder with no name. ");
                setToast("Failed to create folder with no name. ");
                return;
            }

            if (mAmbitoSelected.getValue().getFolder(folderName) != null) {
                Log.w(TAG, "Failed to create folder " + folderName + ": folder already exists. ");
                setToast("Folder " + folderName + " already exists.");
                return;
            }
            mAmbitoSelected.getValue().addFolder(folderName);           // Añadimos una nueva Carpeta al Ámbito seleccionado
            mAmbitoSelected.setValue(mAmbitoSelected.getValue());       // Actualizamos la colección de carpetas del Ámbito seleccionado
            setToast("Folder " + folderName + " correctly created.");   // Creamos Toast informativo

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to add folder " + folderName + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    /**
     * Añadimos una nueva Nota a la lista de Notas del Ámbito actual (también en la Carpeta actual)
     * @param noteName Título de la Nota a añadir.
     * @param text_plain Texto plano de la Nota a añadir.
     * @param text_html Texto en formato HTML de la Nota a añadir.
     * @param audios booleano para determinar si la nota tiene audios
     * @param audiosID identificador de la colección audios de la nota
     * @throws NullPointerException Si el Ámbito actual no ha sido correctamente seleccionado.
     */
    public void addNote(String noteName, String text_plain, String text_html, Boolean images, Boolean documents, Boolean audios, String documentsID, String imagesID, String audiosID) {
        try {
            Note newNote = new Note(noteName, text_plain, text_html);   // Creamos una nueva Nota
            newNote.setDocumentsID(documentsID);
            newNote.setImagesID(imagesID);
            newNote.setAudiosID(audiosID);
            newNote.setHaveAudios(audios);
            newNote.setHaveDocuments(documents);
            newNote.setHaveImages(images);

            // Si tenemos una carpeta seleccionada, la asignamos a la Nota
            if (mFolderSelected.getValue() != null) newNote.setFolderTAG(mFolderSelected.getValue().getName());

            mAmbitoSelected.getValue().addNote(newNote);                // Añadimos esa Nota al Ámbito seleccionado
            mFolderSelected.setValue(mFolderSelected.getValue());       // Actualizamos la colección de Notas de la Folder seleccionada
            databaseAdapter.saveNote(newNote);                          // Guardamos la Nota en DB
            setToast("Note " + noteName + " correctly created.");       // Creamos Toast Informativo

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to add note " + noteName + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }


    //******************
    //      EDIT's
    //******************

    /**
     * Actualizamos el contenido del Ambito editado
     * @param ambitoName Nombre del Ambito editado.
     * @param ambitoColor Color del Ámbito editado.
     * @throws NullPointerException Si el Ambito editado no ha sido correctamente seleccionado.
     */
    public void editAmbito(String ambitoID, String ambitoName, int ambitoColor) {
        try {
            for (Ambito ambito : mUserSelected.getValue().getAmbitos()) {
                if (ambito.getSelfID().equals(ambitoID)) {
                    ambito.setName(ambitoName);
                    ambito.setColor(ambitoColor);
                    if (mAmbitoSelected.getValue().getSelfID().equals(ambitoID)) mViewUpdated.setValue(false);         //Actualizamos la Vista solo si es el mismo Ambito para cargar el Tema
                    if (mAmbitoSelected.getValue().getSelfID().equals(ambitoID)) mAmbitoSelected.setValue(ambito);     // Actualizamos el Ambito editado
                    mUserSelected.setValue(mUserSelected.getValue());
                    // Guardamos el Ambito en DB
                    databaseAdapter.saveAmbito(ambito);
                    setToast("Ambito " + ambitoName + " correctly edited.");                                          // Creamos Toast Informativo
                    return;
                }
            }
        } catch (NullPointerException exception) {
            Log.w(TAG, "Failed to edit ambito " + ambitoName + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    /**
     * Actualizamos el contenido del Ambito editado
     * @param name Nombre del user editado.
     * @param apellidos Apellidos del user editado.
     * @param email Mail del user editado.
     * @param password Contraseña del user editado.
     * @throws NullPointerException Si el User editado no ha sido correctamente seleccionado.
     */
    public void editUser(String name, String apellidos, String email, String password) {
        try {
            User selected = mUserSelected.getValue();
            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();

            authUser.updateEmail(email);
            authUser.updatePassword(password);
            FirebaseAuth.getInstance().updateCurrentUser(authUser);

            selected.setFirst(name);
            selected.setLast(apellidos);
            selected.setMail(email);
            selected.setPassword(password);

            DatabaseAdapter.getInstance().saveUser(selected);                                                 // Guardamos el Ambito en DB
            setToast("User " + email + " correctly edited.");    // Creamos Toast Informativo

        } catch (NullPointerException exception) {
            Log.w(TAG, "Failed to edit user " + name + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    /**
     * Actualizamos el contenido de la Nota editada del Ámbito actual.
     * @param title Título de la Nota editada.
     * @param plainText Texto plano de la Nota editada.
     * @param htmlText Texto en formato HTML de la Nota editada.
     * @param audios booleano para determinar si la nota tiene audios
     * @param audiosID identificador de la colección audios de la nota
     * @throws NullPointerException Si la Nota Editada no ha sido correctamente seleccionada.
     */
    public void editNote(String title, String plainText, String htmlText, boolean images, boolean documents, Boolean audios, String documentsID, String imagesID, String audiosID){
        Note selected = mNoteSelected.getValue();                   // Editamos la Nota seleccionada
        selected.setTitle(title);
        selected.setText_plain(plainText);
        selected.setText_html(htmlText);
        selected.setDocumentsID(documentsID);
        selected.setImagesID(imagesID);
        selected.setLastUpdate(new Date());
        selected.setHaveImages(images);
        selected.setHaveDocuments(documents);
        selected.setAudiosID(audiosID);
        selected.setHaveAudios(audios);
        mNoteSelected.setValue(mNoteSelected.getValue());           // Actualizamos la Nota seleccionada
        mFolderSelected.setValue(mFolderSelected.getValue());       // Actualizamos colección de la carpeta seleccionada
        databaseAdapter.saveNote(selected);                         // Guardamos la Nota en DB
        setToast("Note " + title + " correctly edited.");           // Creamos Toast Informativo
    }


    //*******************
    //     DELETE's
    //*******************

    /** Eliminamos una nota de la colección de notas del Ámbito seleccionado. La nota se elimina del Ámbito
     * y de la carpeta contenedora, en caso de que haya alguna.
     * @param noteID ID de la nota a eliminar.
     * @throws NullPointerException Si el Ámbito de la Nota no ha sido correctamente seleccionado.
     */
    public void deleteNote(String noteID) {
        try {
            for (Note note :mAmbitoSelected.getValue().getNotes()) {
                if (note.getSelfID().equals(noteID)) {
                    mAmbitoSelected.getValue().removeNote(note);                    // Eliminamos esa Nota del Ámbito seleccionado
                    mFolderSelected.setValue(mFolderSelected.getValue());           // Actualizamos la colección de Notas de la Folder seleccionada
                    // Si la Nota eliminada es la seleccionada, la deseleccionamos.
                    if (mNoteSelected.getValue().getSelfID().equals(noteID)) mNoteSelected.setValue(null);

                    databaseAdapter.deleteNote(note.getSelfID());                   // Eliminamos la Nota de DB
                    if(note.getHaveImages()) databaseAdapter.deleteImages(note.getImagesID());          //Eliminamos el Array de Imagenes de la DB
                    if(note.getHaveDocuments()) databaseAdapter.deleteDocuments(note.getDocumentsID());    //Eliminamos el Array de Documentos de la DB
                    if(note.getHaveAudios()) databaseAdapter.deleteAudios(note.getAudiosID());
                    setToast("Note " + note.getTitle() + " correctly deleted.");    // Creamos Toast Informativo
                    return;
                }
            }
            Log.w(TAG, "Failed to delete note " + noteID + ": invalid ID.");

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to delete note " + noteID + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }


    /**
     * Eliminamos todas las notas de la colección de notas de la carpeta seleccionada. Estas son eliminadas
     * tanto del Ámbito como de la Carpeta. Además, eliminamos la carpeta de la colección de Carpetas del
     * Ámbito seleccionado.
     * @param folderName Carpeta Seleccionada
     * @throws NullPointerException Si el Ámbito de la Carpeta no ha sido correctamente seleccionado.
     */
    public void deleteFolder(String folderName) {
        try {
            if (mAmbitoSelected.getValue().getFolder(folderName) != null) {
                mAmbitoSelected.getValue().removeFolder(folderName);            // Eliminamos la Carpeta  del Ámbito seleccionado en modo local
                mAmbitoSelected.setValue(mAmbitoSelected.getValue());           // Actualizamos la colección de carpetas del Ámbito seleccionado

                // Si la carpeta eliminada es la carpeta seleccionada, la deseleccionamos.
                if (mFolderSelected.getValue().getName().equals(folderName)) mFolderSelected.setValue(null);
                databaseAdapter.deleteFolder(folderName);                       // Eliminamos las Notas de la Carpeta de DB
                setToast("Folder " + folderName + " correctly deleted.");       // Creamos Toast informativo
                return;
            }
            Log.w(TAG, "Failed to delete folder " + folderName + ": folder not founded.");

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to delete folder " + folderName + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }


    /**
     * Eliminamos un Ámbito del Usuario. Ello implica eliminar TODAS las notas que contiene, así como sus Carpetas.
     * Solo podemos eliminar un Ámbito en caso que el Usuario tenga más de uno.
     *
     * @param ambitoID ID del Ámbito a eliminar.
     * @throws NullPointerException Si el Usuario del Ámbito no ha sido correctamente registrado.
     */
    public void deleteAmbito(String ambitoID) {
        try {
            if (mUserSelected.getValue().getAmbitos().size() > 1) {

                for (Ambito ambito : mUserSelected.getValue().getAmbitos()) {
                    if (ambito.getSelfID().equals(ambitoID)) {
                        mUserSelected.getValue().getAmbitos().remove(ambito);                       // Eliminamos el Ámbito del Usuario registrado en modo Local.
                        mUserSelected.setValue(mUserSelected.getValue());                           // Actualizamos el listado de Ámbitos del Usuario registrado.
                        databaseAdapter.deleteAmbito(ambitoID);                                     // Eliminamos el Ámbito de DB

                        setToast("Ambito " + ambito.getName() + " correctly deleted.");             // Creamos Toast informativo
                        // Finalmente, si el Ámbito eliminado es el seleccionado, lo deseleccionamos.
                        if (mAmbitoSelected.getValue().getSelfID().equals(ambitoID)) selectAmbito(mUserSelected.getValue().getAmbitos().get(0).getName());   // Seleccionamos el primer Ámbito de la colección de Ámbitos del Usuario.
                        return;
                    }
                } Log.w(TAG, "Failed to delete ambito " + ambitoID + ": ambito note founded. ");

            } else setToast("Failed to delete last User's Ambito");

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to add ambito " + ambitoID + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }


    //*******************
    //      AMBITOS
    //*******************

    /**
     * Actualizamos la posición de los Ambitos en el RecyclerView
     * @throws NullPointerException Si la Nota Editada no ha sido correctamente seleccionada.
     */
    public void savePositionAmbitos() {
        try {
            for (Ambito ambito : mUserSelected.getValue().getAmbitos()) {
                databaseAdapter.saveAmbito(ambito);
            }
        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to save positions: null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }


    //*******************
    //       NOTAS
    //*******************

    /**
     * Duplicamos una nota de la colección de notas de la Carpeta seleccionada.
     * Añadimos una nueva nota con los mismos atributos que la nota con ID 'noteID' (excepto el mismo ID)
     * @param noteID ID de la nota a duplicar.
     * @throws NullPointerException Si el Ámbito actual no ha sido correctamente seleccionado.
     */
    public void copyNote(String noteID) {
        try {
            for (Note note :mAmbitoSelected.getValue().getNotes()) {
                if (note.getSelfID().equals(noteID)) {
                    Note duplicated = new Note(note.getTitle(), note.getText_plain(), note.getText_html());
                    duplicated.setFolderTAG(note.getFolderTAG());

                    duplicated.setHaveImages(note.getHaveImages());
                    if (duplicated.getHaveImages())
                        duplicated.setImagesID(documentManager.copyImages(note.getImagesID()));

                    duplicated.setHaveDocuments(note.getHaveDocuments());
                    if (duplicated.getHaveDocuments())
                        duplicated.setDocumentsID(documentManager.copyDocuments(note.getDocumentsID()));

                    duplicated.setHaveAudios(note.getHaveAudios());
                    if (duplicated.getHaveAudios())
                        duplicated.setAudiosID(documentManager.copyAudios(note.getAudiosID()));

                    mAmbitoSelected.getValue().addNote(duplicated);                 // Añadimos esa Nota al Ámbito seleccionado
                    mFolderSelected.setValue(mFolderSelected.getValue());           // Actualizamos la colección de Notas de la Folder seleccionada
                    databaseAdapter.saveNote(duplicated);                           // Guardamos la Nota en DB
                    setToast("Note " + note.getTitle() + " correctly duplicated."); // Creamos Toast Informativo
                    return;
                }
            }
            Log.w(TAG, "Failed to copy note " + noteID + ": invalid ID.");

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to copy note " + noteID + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    /**
     * Mueve la nota definida por el ID 'noteID', del Ámbito seleccionado al Ámbito dado por 'ambitoID'.
     * Además, la añade a la Carpeta 'folderTAG' del Ámbito de destino 'ambitoID'.
     * @param ambitoID ID del Ámbito de destino de la nota 'noteID'.
     * @param folderTAG Nombre de la Carpeta de destino, dentro del ÁmbitoID, de la nota 'noteID'.
     * @param noteID ID de la nota a mover.
     * @throws NullPointerException Si el Ámbito actual no ha sido correctamente seleccionado.
     */
    public void moveNote(String ambitoID, String folderTAG, String noteID) {
        try {
            Note selectedNote = null;
            Ambito selectedAmbito = null;

            // Obtenemos el Ámbito del Usuario seleccionado
            for (Ambito ambito : mUserSelected.getValue().getAmbitos())
                if (ambito.getSelfID().equals(ambitoID)){ selectedAmbito = ambito; break; }

            // Obtenemos la Nota del Ámbito seleccionado
            for (Note note :mAmbitoSelected.getValue().getNotes())
                if (note.getSelfID().equals(noteID)) { selectedNote = note; break; }

            if (selectedNote != null && selectedAmbito != null) {
                mAmbitoSelected.getValue().removeNote(selectedNote);        // Quitamos la Nota de la colección de Notas del Ámbito seleccionado.
                selectedNote.setFolderTAG(folderTAG);                       // Seteamos el TAG de la Carpeta de destino de la Nota.
                selectedAmbito.addNote(selectedNote);                       // Añadimos la Nota al Ámbito de destino.

                mFolderSelected.setValue(mFolderSelected.getValue());       // Actualizamos la colección de Notas de la Folder seleccionada
                if (mNoteSelected.getValue().getSelfID().equals(noteID))    // Si la Nota movida es la seleccionada, la deseleccionamos.
                    mNoteSelected.setValue(null);
                databaseAdapter.saveNote(selectedNote);                     // Finalmente, guardamos la Nota en DB

                setToast("Note " + selectedNote.getTitle() + " correctly moved");   // Toast informativo

            } else Log.w(TAG, "Failed to move note " + noteID + ": invalid "
                    + ((selectedAmbito == null) ? "AmbitoID" : "NoteID"));

        } catch(NullPointerException exception) {
            Log.w(TAG, "Failed to move note " + noteID + ": null pointer exception.");
            Log.w(TAG, "Exception message: " + exception.getMessage());
        }
    }

    /**
     * Metodo para establecer los valores del Toast
     * @param s Valores del Toast
     */
    private void setToast(String s) {
        Log.w(TAG, s);
        mToast.setValue(s);
    }


    /* BUILDER PATTERN FOR DATABASE INTERFACE */
    protected class UserBuilder implements DatabaseAdapter.LoaderInterface{

        private User currentUser;
        private int loadingCounter;

        /**
         * Constructor de la clase
         */
        public UserBuilder(){
            Log.w("UserBuilder", "Beginning user building process...");
            currentUser = null;
            loadingCounter = 0;
        }

        /**
         * Metodo para conseguir la informacion Usuario logueado
         * @param user Usuario
         */
        @Override
        public void getUserResult(User user) {
            currentUser = user;
            Log.w("UserBuilder", "Step 1 succes: user correctly loaded from Database.");
            databaseAdapter.getAmbitos();
        }

        /**
         * Metodo para conseguir la coleccion Ambitos del User
         * @param userID
         * @param userAmbitos
         */
        @Override
        public void getAmbitoCollectionResult(String userID, ArrayList<Ambito> userAmbitos) {
            if (currentUser != null && currentUser.getSelfID().equals(userID)) {
                currentUser.setAmbitos(userAmbitos);
                Log.w("UserBuilder", "Step 2 succes: ambitos of user " + currentUser.getSelfID() + " correctly loaded from Database.");
                for (Ambito ambito : userAmbitos) databaseAdapter.getNotes(ambito.getSelfID());
            } else Log.w("UserBuilder", "Step 2 failure. User " + currentUser.getSelfID() + " it's unitiallized.");
        }

        /**
         * Metodo para conseguir la coleccion Notes del User
         * @param ambitoID ID del Ambito de las Notas
         * @param ambitoNotes Notas del Ambito
         */
        @Override
        public void getNoteCollectionResult(String ambitoID, ArrayList<Note> ambitoNotes) {
            if (currentUser.getAmbitos().isEmpty())
                Log.w("UserBuilder", "Step 3 failure: Ambito " + ambitoID + " it's unitiallized.");
            else {
                for (Ambito ambito : currentUser.getAmbitos()) {
                    if (ambito.getSelfID().equals(ambitoID)) {
                        for (Note note : ambitoNotes){ // Cargamos las Imágenes y Documentos necesarios de DB en el Map de DocumentManager
                            if (note.getHaveImages() && note.getImagesID() != null) documentManager.getImagesNote(note.getImagesID());
                            if (note.getHaveDocuments() && note.getDocumentsID() != null)  documentManager.getDocuments(note.getDocumentsID());
                            if (note.getHaveAudios() != null)
                                if(note.getHaveAudios() && note.getAudiosID() != null)
                                    documentManager.getAudios(note.getAudiosID());
                            ambito.addNote(note);
                        }
                        loadingCounter++;
                        break;
                    }
                }
                // If notes for all user ambitos have been set, call the owner class for setting the user
                if (loadingCounter == currentUser.getAmbitos().size()) {
                    Log.w("UserBuilder", "Step 3 succes: all notes of user " + currentUser.getSelfID() + " correctly loaded from Database.");
                    setToast("User " + currentUser.getMail() + " correctly logged.");
                    mUserSelected.setValue(currentUser);
                    selectAmbito(currentUser.getAmbitos().get(0).getName());
                }
            }
        }

        /**
         * Metodo para establecer el Toast
         * @param s Mensaje del Toast
         */
        @Override
        public void setToast(String s) {
            MainViewModel.this.setToast(s);
        }
    }
}