package ru.sbt.edu_power.e2e_core.smoke_layout;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.PageUniqueId;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class PageIdGenerator {
    private static final String MODULE_DIR = System.getProperty("user.dir");
    private final Map<String, File> pagesToPathMap = new HashMap<>();

    public PageIdGenerator() {
        final File root = new File(MODULE_DIR);
        final File pagesRoot = getPagesRoot(root);
        if (Objects.isNull(pagesRoot)) {
            throw new AutotestError("Не найдена директория pages");
        }
        collectPages(pagesRoot);
    }

    public Map<String, File> getPagesToPathMap() {
        return pagesToPathMap;
    }

    @SneakyThrows
    public String generateId(final Class<? extends Page> page) {
        final JavaParser parser = new JavaParser();
        if (!pagesToPathMap.containsKey(page.getSimpleName())) {
            throw new AutotestError("Не найден файл класса " + page.getName());
        }
        final ParseResult<CompilationUnit> result = parser.parse(pagesToPathMap.get(page.getSimpleName()));
        if (!result.isSuccessful()) {
            log.error("Произошёл сбой при парсинге класса {}", pagesToPathMap.get(page.getSimpleName()));
            log.error("{}", result.getProblems().stream().map(Problem::toString).collect(Collectors.joining("\n")));
            throw new AutotestError("Ошибка парсинга");
        }

        final String uuid = UUID.randomUUID().toString();
        final Expression expression = new StringLiteralExpr(uuid);
        final Name annotationName = new Name(PageUniqueId.class.getSimpleName());
        final MemberValuePair memberValuePair = new MemberValuePair();
        memberValuePair.setName(new SimpleName("pageId"));
        memberValuePair.setValue(expression);
        final NodeList<MemberValuePair> annotationParamList = new NodeList<>();
        annotationParamList.add(memberValuePair);
        final AnnotationExpr annotationExpr = new NormalAnnotationExpr(annotationName, annotationParamList);
        result.getResult().ifPresent(cu -> cu.addImport(PageUniqueId.class));
        result.getResult().get()
              .getLocalDeclarationFromClassname(page.getSimpleName())
              .get(0)
              .addAnnotation(annotationExpr);
        FileUtils.write(
                pagesToPathMap.get(page.getSimpleName()),
                result.getResult().get().toString(new DefaultPrinterConfiguration()),
                StandardCharsets.UTF_8
        );
        return uuid;
    }

    private File getPagesRoot(final File position) {
        if ("pages".equals(position.getName())) {
            return position;
        }
        return Arrays
                .stream(Objects.requireNonNull(position.listFiles()))
                .filter(child -> !"target".equals(child.getName()))
                .filter(File::isDirectory)
                .map(this::getPagesRoot)
                .filter(result -> !Objects.isNull(result))
                .findFirst()
                .orElse(null);
    }

    private void collectPages(final File position) {
        if (position.isFile() && position.getName().endsWith(".java")) {
            final String className = position.getName().split("\\.")[0];
            if (pagesToPathMap.containsKey(className)) {
                throw new AutotestError("Обнаружены дубликаты названий классов страниц " + position.getName());
            }
            pagesToPathMap.put(className, position);
            return;
        }
        final File[] children = position.listFiles();
        if (Objects.isNull(children)) {
            return;
        }
        for (final File child : children) {
            collectPages(child);
        }
    }
}
