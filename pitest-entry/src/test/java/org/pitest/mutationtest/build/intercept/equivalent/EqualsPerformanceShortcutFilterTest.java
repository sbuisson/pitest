package org.pitest.mutationtest.build.intercept.equivalent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.functional.predicate.True;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.config.Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator;

public class EqualsPerformanceShortcutFilterTest {

  ClassByteArraySource            source = ClassloaderByteArraySource
      .fromContext();
  EqualsPerformanceShortcutFilter testee = new EqualsPerformanceShortcutFilter();

  final GregorMutater mutator = createMutator(
      Mutator.byName("REMOVE_CONDITIONALS"));
  
  @Test
  public void shouldDeclareTypeAsFilter() {
    assertThat(this.testee.type()).isEqualTo(InterceptorType.FILTER);
  }
  
  @Test
  public void shouldNotFilterShortCutMutantsNotInEqualsMethods() {
    final List<MutationDetails> mutations = mutator
        .findMutations(ClassName.fromClass(HasNonOveridingEquals.class));
    assertThat(mutations).hasSize(2);

    this.testee.begin(forClass(HasNonOveridingEquals.class));
    final Collection<MutationDetails> actual = this.testee.intercept(mutations,
        mutator);
    this.testee.end();
    
    assertThat(actual).hasSize(2);
  }

  @Test
  public void shouldNotFilterGeneralMutantsInEqualMethods() {
    final GregorMutater mutator = createMutator(
        ReturnValsMutator.RETURN_VALS_MUTATOR);
    final List<MutationDetails> mutations = mutator
        .findMutations(ClassName.fromClass(HasNonShortCutEquals.class));
    assertThat(mutations).hasSize(1);

    this.testee.begin(forClass(HasNonShortCutEquals.class));
    final Collection<MutationDetails> actual = this.testee.intercept(mutations,
        mutator);
    this.testee.end();

    assertThat(actual).hasSize(1);
  }
  
  @Test
  public void shouldFilterShortCutEqualsMutantsInEqualMethods() {
    final List<MutationDetails> mutations = mutator
        .findMutations(ClassName.fromClass(HasShortCutEquals.class));
    assertThat(mutations).hasSize(12);

    this.testee.begin(forClass(HasShortCutEquals.class));
    final Collection<MutationDetails> actual = this.testee.intercept(mutations,
        mutator);
    this.testee.end();
    
    assertThat(actual).hasSize(11);
  }

  GregorMutater createMutator(MethodMutatorFactory... factories) {
    final Collection<MethodMutatorFactory> mutators = Arrays.asList(factories);
    return createMutator(mutators);
  }

  GregorMutater createMutator(Collection<MethodMutatorFactory> factories) {
    return new GregorMutater(this.source, True.<MethodInfo> all(), factories);
  }

  ClassTree forClass(Class<?> clazz) {
    final byte[] bs = this.source.getBytes(clazz.getName()).value();
    return ClassTree.fromBytes(bs);
  }
}

class HasNonShortCutEquals {
  @Override
  public boolean equals(Object other) {
    return other.getClass().isAssignableFrom(this.getClass());
  }
}

class HasNonOveridingEquals {
  public boolean equals(HasNonOveridingEquals obj) {
    if (this == obj) {
      return true;
    }
    return false;
  }
}

class HasShortCutEquals {
  private String s;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final HasShortCutEquals other = (HasShortCutEquals) obj;
    if (this.s == null) {
      if (other.s != null) {
        return false;
      }
    } else if (!this.s.equals(other.s)) {
      return false;
    }
    return true;
  }

}
