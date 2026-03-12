package no.nav.aura.envconfig.model.resource;

import static no.nav.aura.envconfig.util.BytesHelper.cloneOrNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.util.BytesHelper;

import org.apache.commons.io.IOUtils;

@SuppressWarnings("serial")
@Entity
public class FileEntity extends ModelEntity {

    @Column(name = "file_name")
    private String name;

    @Lob
    private byte[] fileData;

    public FileEntity() {
    }

    public FileEntity(String name) {
        this.name = name;
    }

    public FileEntity(String name, InputStream file) {
        this.name = name;
        read(file);
    }

    public FileEntity(FileEntity other) {
        this.name = other.name;
        this.fileData = BytesHelper.cloneOrNull(other.fileData);
    }

    public final void read(InputStream input) {
        try {
            setFileData(IOUtils.toByteArray(input));
        } catch (IOException e) {
            throw new RuntimeException("Error reading input ", e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    public void write(OutputStream out) {
        try {
            out.write(getFileData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getFileData() {
        return cloneOrNull(fileData);
    }

    public void setFileData(byte[] fileData) {
        this.fileData = cloneOrNull(fileData);
    }

}
