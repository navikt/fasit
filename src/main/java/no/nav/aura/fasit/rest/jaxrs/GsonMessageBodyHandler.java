package no.nav.aura.fasit.rest.jaxrs;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.*;
import org.jboss.resteasy.spi.ReaderException;
import org.jboss.resteasy.spi.WriterException;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Component
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GsonMessageBodyHandler implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    private static final String UTF_8 = "UTF-8";

    private Gson gson;

    public GsonMessageBodyHandler() {
     // Customize the gson behavior here
        final GsonBuilder gsonBuilder = new GsonBuilder();
        Converters.registerAll(gsonBuilder);
        gson = gsonBuilder.disableHtmlEscaping()
                .setFieldNamingStrategy(f -> f.getName().toLowerCase())
                .registerTypeHierarchyAdapter(Enum.class, new IgnoreCaseEnumDeserializer<>())
                .setPrettyPrinting()
                .create();
    }
    
    public Gson getGson() {
        return gson;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) {

        Type jsonType;
        if (type.equals(genericType)) {
            jsonType = type;
        } else {
            jsonType = genericType;
        }

        try (InputStreamReader streamReader = new InputStreamReader(entityStream, UTF_8)) {

            return gson.fromJson(streamReader, jsonType);
        } catch (Exception e) {
            throw new ReaderException(e);
        }

    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public long getSize(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        try (OutputStreamWriter writer = new OutputStreamWriter(entityStream, UTF_8)) {
            Type jsonType = type;
            if (genericType != null && !type.equals(genericType)) {
                jsonType = genericType;
            }
            gson.toJson(object, jsonType, writer);
        } catch (Exception e) {
            throw new WriterException(e);
        }

    }
    
    private class IgnoreCaseEnumDeserializer<T extends Enum<T>> implements JsonDeserializer<T>, JsonSerializer<T>{

        @Override
        public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            @SuppressWarnings("unchecked")
            IgnoreCaseEnumConverter<T> converter = new IgnoreCaseEnumConverter<>((Class<T>) type);
            return converter.fromString(json.getAsString());
        }

        @Override
        public JsonElement serialize(T src, Type type, JsonSerializationContext context) {
            @SuppressWarnings("unchecked")
            IgnoreCaseEnumConverter<T> converter = new IgnoreCaseEnumConverter<>((Class<T>) type);
            return context.serialize(converter.toString(src));
        }
        
    }


}
