package org.leeminkan.bookstore.mapper;

import org.leeminkan.bookstore.dto.BookResponse;
import org.leeminkan.bookstore.dto.CreateBookRequest;
import org.leeminkan.bookstore.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring") // Tells MapStruct to make this a Spring Bean
public interface BookMapper {

    // This method signature is all we write.
    // MapStruct generates the code to transfer fields from CreateBookRequest to Book.
    Book toEntity(CreateBookRequest request);

    // If a property name is different (e.g., DTO has 'title', Entity has 'bookTitle'),
    // you would use @Mapping to guide the conversion:
    // @Mapping(source = "dtoTitle", target = "entityBookTitle")
    // Book toEntity(CreateBookRequest request);

    // New method: MapStruct automatically generates the logic for list conversion!
    List<BookResponse> toResponseList(List<Book> books);
}