package org.example.jvspringbootfirstbook.service.book;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.jvspringbootfirstbook.dto.book.BookDto;
import org.example.jvspringbootfirstbook.dto.book.BookSearchParametersDto;
import org.example.jvspringbootfirstbook.dto.book.CreateBookRequestDto;
import org.example.jvspringbootfirstbook.exception.EntityNotFoundException;
import org.example.jvspringbootfirstbook.mapper.BooksMapper;
import org.example.jvspringbootfirstbook.model.Book;
import org.example.jvspringbootfirstbook.repository.book.BookRepository;
import org.example.jvspringbootfirstbook.repository.book.BookSpecificationBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final BooksMapper bookMapping;
    private final BookSpecificationBuilder bookSpecificationBuilder;

    @Override
    public BookDto save(CreateBookRequestDto createBookRequestDto) {
        Book book = bookMapping.toModel(createBookRequestDto);
        return bookMapping.toBookDto(bookRepository.save(book));
    }

    @Override
    public List<BookDto> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable).stream()
                .map(bookMapping::toBookDto)
                .toList();
    }

    @Override
    public BookDto findById(Long id) {
        return bookMapping.toBookDto(
                bookRepository.findById(id).orElseThrow(
                        () -> new EntityNotFoundException("Can't find book with id: " + id)));
    }

    @Override
    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }

    @Override
    public BookDto update(Long id, CreateBookRequestDto createBookRequestDto) {
        if (!bookRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Can't find book with id: " + id);
        }
        Book book = bookMapping.toModel(createBookRequestDto);
        book.setId(id);
        return bookMapping.toBookDto(bookRepository.save(book));
    }

    @Override
    public List<BookDto> searchBooks(BookSearchParametersDto searchParameters, Pageable pageable) {
        Specification<Book> bookSpecification = bookSpecificationBuilder.build(searchParameters);
        return bookRepository.findAll(bookSpecification, pageable)
                .stream().map(bookMapping::toBookDto).toList();
    }
}
